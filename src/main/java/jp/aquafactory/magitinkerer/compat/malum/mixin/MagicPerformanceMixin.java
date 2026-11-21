package jp.aquafactory.magitinkerer.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.malum.MalumSoulhunter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MagicPerformanceHelper.class, remap = false)
public abstract class MagicPerformanceMixin {
    @WrapOperation(method = "applyRawMagicDamage", at = @At(value = "INVOKE",
            target = "Lnet/stln/magitech/helper/CombatHelper;applyDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private static void magitinkerer$keepSpellWeapon(LivingEntity damageCaster, Entity target, DamageSource source,
                                                     float effectiveDamage, Operation<Void> original, LivingEntity caster,
                                                     ItemStack wand) {
        MalumSoulhunter.withWeapon(target, source, wand, () -> original.call(damageCaster, target, source, effectiveDamage));
    }
}
