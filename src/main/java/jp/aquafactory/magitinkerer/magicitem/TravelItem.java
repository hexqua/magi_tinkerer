package jp.aquafactory.magitinkerer.magicitem;

import jp.aquafactory.magitinkerer.magicitem.client.TravelItemClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.component.ManaContainerComponent;
import net.stln.magitech.content.item.tool.toolitem.SpellCasterItem;
import net.stln.magitech.content.item.tooltip_item.TooltipTextItem;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.feature.magic.cooldown.CooldownHelper;
import net.stln.magitech.feature.magic.spell.ISpell;
import net.stln.magitech.feature.magic.spell.SpellInit;
import net.stln.magitech.helper.ComponentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;

public final class TravelItem extends TooltipTextItem {
    private static final int USE_DURATION = 72_000;
    private static final int ABSORPTION_INTERVAL = 10;
    private static final int ITEM_COOLDOWN = 200;
    private static final String THREAD_DENIED_DIMENSION = "message.magitinkerer.signum_thread.denied_dimension";
    private static final String MIRROR_DIFFERENT_DIMENSION = "message.magitinkerer.spatial_distortion_mirror.different_dimension";
    private static final String UNSAFE_DESTINATION = "message.magitinkerer.common.magic_item.unsafe_destination";
    private static final String NOT_ENOUGH_MANA = "spell.magitech.hint.not_enough_mana";
    private static final String COOLING_DOWN = "spell.magitech.hint.cooling_down";
    private static final WeakHashMap<ServerPlayer, Casting> CASTS = new WeakHashMap<>();

    enum Kind {
        THREAD, SPATIAL, DIMENSIONAL
    }

    private final Kind kind;

    TravelItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1).component(ComponentInit.MANA_CONTAINER_COMPONENT.get(),
                new ManaContainerComponent(0, kind == Kind.THREAD ? 100_000 : 300_000,
                        kind == Kind.THREAD ? 1_000 : 5_000)));
        this.kind = kind;
    }

    public boolean isMirror() {
        return kind != Kind.THREAD;
    }

    public ISpell spell() {
        return isMirror() ? SpellInit.TENEBPORT.asSpell() : SpellInit.LUXGRAIL.asSpell();
    }

    public static @Nullable ItemStack heldWand(Player player) {
        if (player.getMainHandItem().getItem() instanceof SpellCasterItem) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof SpellCasterItem ? player.getOffhandItem() : null;
    }

    public long manaCost(@Nullable Player player) {
        float cost = player == null ? spell().getConfig().cost()
                : MagicPerformanceHelper.getEffectiveCost(player, heldWand(player), spell());
        return Float.isFinite(cost) && cost >= 0 ? (long) (cost * 1_000L) : Long.MAX_VALUE;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || player.isUsingItem()) {
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel targetLevel = isMirror() ? mirrorDestination(serverPlayer).newLevel() : serverPlayer.serverLevel();
        String failure = failure(serverPlayer, stack, targetLevel);
        if (failure != null) {
            showFailure(serverPlayer, failure);
            return InteractionResultHolder.fail(stack);
        }
        int duration = Math.clamp((int) MagicPerformanceHelper.getEffectiveChargeTime(player, heldWand(player), spell()),
                1, USE_DURATION - 1);
        player.startUsingItem(hand);
        if (!player.isUsingItem()) {
            return InteractionResultHolder.fail(stack);
        }
        CASTS.put(serverPlayer, new Casting(stack, hand, serverPlayer.serverLevel(), duration));
        TravelEffects.begin(serverPlayer, this, duration);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Casting cast = CASTS.get(player);
        if (cast == null || !player.isAlive() || cast.stack() != stack
                || player.getItemInHand(cast.hand()) != stack || cast.level() != level) {
            endCharge(player);
            player.stopUsingItem();
            return;
        }
        if (USE_DURATION - remaining + 1 >= cast.duration()) {
            endCharge(player);
            player.stopUsingItem();
            finishCast(player, stack);
        }
    }

    @Override
    public void onStopUsing(@NotNull ItemStack stack, @NotNull LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer player) {
            endCharge(player);
        }
    }

    private static void endCharge(ServerPlayer player) {
        if (CASTS.remove(player) != null) {
            TravelEffects.sendCharge(player, 0);
        }
    }

    private void finishCast(ServerPlayer player, ItemStack stack) {
        ServerLevel origin = player.serverLevel();
        Vec3 from = player.position();
        DimensionTransition target = isMirror() ? mirrorDestination(player) : threadDestination(player);
        String failure = failure(player, stack, target.newLevel());
        if (failure != null) {
            showFailure(player, failure);
            return;
        }
        long cost = manaCost(player);
        int cooldown = MagicPerformanceHelper.getEffectiveCooldown(player, heldWand(player), spell());
        if (player.changeDimension(target) == null || player.serverLevel() != target.newLevel()) {
            showFailure(player, UNSAFE_DESTINATION);
            return;
        }
        player.fallDistance = 0;
        ComponentHelper.updateMana(stack, mana -> mana - cost);
        CooldownHelper.updateCooldown(player, spell(), cooldown);
        player.getCooldowns().addCooldown(this, ITEM_COOLDOWN);
        TravelEffects.complete(player, this, origin, from);
    }

    private @Nullable String failure(ServerPlayer player, ItemStack stack, ServerLevel targetLevel) {
        if (!isMirror() && !TravelItemContent.allowsThread(player.level().dimension().location())) {
            return THREAD_DENIED_DIMENSION;
        }
        if (kind == Kind.SPATIAL && targetLevel != player.serverLevel()) {
            return MIRROR_DIFFERENT_DIMENSION;
        }
        if (CooldownHelper.isCooldown(player, spell()) || player.getCooldowns().isOnCooldown(this)) {
            return COOLING_DOWN;
        }
        return ComponentHelper.getMana(stack) < manaCost(player) ? NOT_ENOUGH_MANA : null;
    }

    private static DimensionTransition mirrorDestination(ServerPlayer player) {
        return player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
    }

    private static DimensionTransition threadDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos column = player.blockPosition();
        Vec3 to = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column));
        return new DimensionTransition(level, to, Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.DO_NOTHING);
    }

    private static void showFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.translatable(message)
                .withColor(message.equals(COOLING_DOWN) ? 0xff8080 : 0xff5555), true);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (!isMirror() && entity instanceof ServerPlayer player && level.getGameTime() % ABSORPTION_INTERVAL == 0) {
            var source = player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY);
            if (source != null) {
                long room = ComponentHelper.getMaxMana(stack) - ComponentHelper.getMana(stack);
                long amount = Math.min(room, ComponentHelper.getMaxFlow(stack) * ABSORPTION_INTERVAL);
                if (amount > 0) {
                    long moved = source.extractMana(amount, false);
                    ComponentHelper.updateMana(stack, mana -> mana + moved);
                }
            }
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean canContinueUsing(ItemStack previous, ItemStack current) {
        // 充填によるクライアント側のスタック更新で詠唱を中断させない。
        // サーバーは onUseTick で開始時のスタックそのものを確認する。
        return previous.getItem() == current.getItem();
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack previous, @NotNull ItemStack current, boolean slotChanged) {
        return slotChanged || previous.getItem() != current.getItem();
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return Optional.of(new ManaTooltip(ComponentHelper.getMana(stack), ComponentHelper.getMaxMana(stack)));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        TravelItemClient.describe(this, stack, lines);
        super.appendHoverText(stack, context, lines, flag);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.max(1, (int) Math.round(13.0 * ComponentHelper.getMana(stack) / ComponentHelper.getMaxMana(stack)));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getBarColor(@NotNull ItemStack stack) {
        return TravelItemClient.barColor(this, stack, Element.MANA.getTextColor().getRGB() & 0xffffff);
    }

    public record ManaTooltip(long mana, long capacity) implements TooltipComponent {}

    private record Casting(ItemStack stack, InteractionHand hand, ServerLevel level, int duration) {}
}
