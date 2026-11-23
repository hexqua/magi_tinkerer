package jp.aquafactory.magitinkerer.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.malum.SpellweavingBlockMatch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.stln.magitech.helper.BlockHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockHelper.class, remap = false)
public abstract class BlockHelperMixin {
    @WrapOperation(method = "getConnectedBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"),
            require = 4, allow = 4)
    private static Block magitinkerer$matchConnectedBlock(BlockState candidate, Operation<Block> original,
                                                          Level level, BlockPos startPos, Block targetBlock, int limit) {
        Block block = original.call(candidate);
        if (block != targetBlock && SpellweavingBlockMatch.matches(level, targetBlock.defaultBlockState(), candidate)) {
            return targetBlock;
        }
        return block;
    }
}
