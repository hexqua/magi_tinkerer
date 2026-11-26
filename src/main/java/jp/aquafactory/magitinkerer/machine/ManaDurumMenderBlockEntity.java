package jp.aquafactory.magitinkerer.machine;

import jp.aquafactory.magitinkerer.magicitem.TravelItem;
import jp.aquafactory.magitinkerer.machine.client.MenderClientEffects;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.stln.magitech.content.block.block_entity.ManaMachineBlockEntity;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.content.sound.SoundInit;
import net.stln.magitech.core.api.mana.flow.ManaFlowRule;
import net.stln.magitech.helper.ComponentHelper;
import org.jetbrains.annotations.NotNull;

public final class ManaDurumMenderBlockEntity extends ManaMachineBlockEntity {
    private static final long MANA_PER_POINT = 5_000;
    private static final int REPAIR_INTERVAL = 10;
    private static final int POINTS_PER_REPAIR = 5;
    private static final int REPAIR_EVENT = 1;
    private static final int SPARK_INTERVAL = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int repairTicks;
    private long nextSparkTick;
    private final IItemHandler automationHandler = new IItemHandler() {
        private final IItemHandler inventory = new InvWrapper(ManaDurumMenderBlockEntity.this);

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot).copy();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack tool = getItem(slot);
            if (tool.isEmpty() || !isFinished(tool)) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    public ManaDurumMenderBlockEntity(BlockPos pos, BlockState state) {
        super(MenderContent.MENDER_ENTITY.get(), pos, state);
    }

    IItemHandler getAutomationHandler() {
        return automationHandler;
    }

    @Override
    public ManaFlowRule getManaFlowRule(BlockState state, Direction side) {
        return side == null || side.getAxis().isVertical() ? ManaFlowRule.insertOnly(-1.0F) : ManaFlowRule.none();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel && !isEmpty()) {
            ItemStack stack = getItem(0);
            if (isMirror(stack)) {
                charge(stack);
            } else if (++repairTicks == REPAIR_INTERVAL) {
                repairTicks = 0;
                repair(serverLevel, pos);
            }
        }
        super.tick(level, pos, state);
    }

    private void charge(ItemStack stack) {
        long room = ComponentHelper.getMaxMana(stack) - ComponentHelper.getMana(stack);
        long amount = Math.min(room, Math.min(ComponentHelper.getMaxFlow(stack), getMana()));
        if (amount > 0) {
            long transferred = getManaHandler(null).consumeMana(amount);
            ComponentHelper.updateMana(stack, mana -> mana + transferred);
            setChanged();
            sendToolUpdate();
        }
    }

    private static boolean isMirror(ItemStack stack) {
        return stack.getItem() instanceof TravelItem item && item.isMirror();
    }

    private static boolean isFinished(ItemStack stack) {
        if (isMirror(stack)) {
            return ComponentHelper.getMana(stack) >= ComponentHelper.getMaxMana(stack);
        }
        return stack.getDamageValue() == 0 && !ComponentHelper.isBroken(stack);
    }

    private void repair(ServerLevel level, BlockPos pos) {
        ItemStack tool = getItem(0);
        int damage = tool.getDamageValue();
        int repaired = (int) Math.min(Math.min(damage, POINTS_PER_REPAIR), getMana() / MANA_PER_POINT);
        if (repaired > 0 && canPlaceItem(0, tool)) {
            getManaHandler(null).consumeMana(repaired * MANA_PER_POINT);
            tool.setDamageValue(damage - repaired);
            // ツールの破損判定を Magitech の耐久しきい値に揃える。
            tool.set(ComponentInit.BROKEN_COMPONENT, tool.getDamageValue() + 1 >= tool.getMaxDamage());
            setChanged();
            sendToolUpdate();
            level.blockEvent(pos, getBlockState().getBlock(), REPAIR_EVENT, 0);

            if (tool.getDamageValue() == 0 && !ComponentHelper.isBroken(tool)) {
                level.playSound(null, pos, SoundInit.GEOMENDING_BREAK.get(), SoundSource.BLOCKS, 0.35F, 1.0F);
            } else if (level.getGameTime() >= nextSparkTick) {
                level.playSound(null, pos, SoundInit.SPARK.get(), SoundSource.BLOCKS, 0.2F, 1.0F);
                nextSparkTick = level.getGameTime() + SPARK_INTERVAL;
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != REPAIR_EVENT) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide) {
            MenderClientEffects.spawnRepairParticles(level, worldPosition);
        }
        return true;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.magitinkerer.mana_durum_mender");
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot == 0 && (isMirror(stack) || stack.getItem() instanceof SynthesisedToolItem
                && stack.isDamageableItem() && ComponentHelper.getPartMaterials(stack).contains(ToolMaterials.DURUSIUM.get()));
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty() || canPlaceItem(slot, stack)) {
            super.setItem(slot, stack.copy());
            repairTicks = 0;
            sendToolUpdate();
        }
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack removed = super.removeItem(slot, amount);
        if (!removed.isEmpty()) {
            repairTicks = 0;
            sendToolUpdate();
        }
        return removed;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        repairTicks = 0;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        return ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sendToolUpdate() {
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
