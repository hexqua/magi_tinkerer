package jp.aquafactory.magitinkerer.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.stln.magitech.content.block.ManaContainerBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ManaDurumMenderBlock extends ManaContainerBlock {
    private static final MapCodec<ManaDurumMenderBlock> CODEC = simpleCodec(ManaDurumMenderBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    public ManaDurumMenderBlock(Properties properties) {
        super(properties, 500_000, 5_000);
    }

    @Override
    protected @NotNull MapCodec<ManaDurumMenderBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                          @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaDurumMenderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level,
            @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTicker(level, type, MenderContent.MENDER_ENTITY.get());
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack held, @NotNull BlockState state,
            @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ManaDurumMenderBlockEntity mender)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (held.isEmpty() && mender.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (held.isEmpty()) {
                player.setItemInHand(hand, mender.removeItem(0, 1));
            } else if (!mender.canPlaceItem(0, held)) {
                player.displayClientMessage(Component.translatable("message.magitinkerer.mana_durum_mender.unsupported",
                        held.getHoverName())
                        .withStyle(ChatFormatting.RED), true);
            } else if (mender.isEmpty()) {
                mender.setItem(0, held.split(1));
            } else if (held.getCount() == 1) {
                ItemStack previous = mender.getItem(0);
                mender.setItem(0, held);
                player.setItemInHand(hand, previous);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ManaDurumMenderBlockEntity mender) {
            Containers.dropContents(level, pos, mender);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
