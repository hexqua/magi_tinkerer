package jp.aquafactory.magitinkerer.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.malum.SpellweavingBlockMatch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.stln.magitech.feature.tool.trait.CollapseTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CollapseTrait.class, remap = false)
public abstract class CollapseTraitMixin {
    @WrapOperation(method = "isWideEnough", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1, allow = 1)
    private static BlockState magitinkerer$matchWidthBlock(Level level, BlockPos pos, Operation<BlockState> original,
                                                           Level traitLevel, BlockState origin, BlockPos originPos) {
        BlockState candidate = original.call(level, pos);
        return SpellweavingBlockMatch.matches(level, origin, candidate) ? origin : candidate;
    }
}
