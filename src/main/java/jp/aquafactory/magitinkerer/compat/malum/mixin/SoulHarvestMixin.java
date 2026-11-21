package jp.aquafactory.magitinkerer.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.core.handlers.SoulHarvestHandler;
import jp.aquafactory.magitinkerer.compat.malum.MalumSoulhunter;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SoulHarvestHandler.class, remap = false)
public abstract class SoulHarvestMixin {
    @WrapOperation(method = "onDeath", at = @At(value = "INVOKE",
            target = "Lcom/sammy/malum/core/handlers/SoulHarvestHandler;dropSpirits(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private static void magitinkerer$addSoulhunterSpoils(LivingEntity target, LivingEntity collector,
                                                        Operation<Void> original, LivingDeathEvent event) {
        MalumSoulhunter.withSpoils(target, collector, event.getSource(), () -> original.call(target, collector));
    }
}
