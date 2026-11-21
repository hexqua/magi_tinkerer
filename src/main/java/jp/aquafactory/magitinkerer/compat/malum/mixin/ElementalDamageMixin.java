package jp.aquafactory.magitinkerer.compat.malum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.malum.MalumSoulhunter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SynthesisedToolItem.class, remap = false)
public abstract class ElementalDamageMixin {
    @WrapOperation(method = "applyElementDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean magitinkerer$keepElementalWeapon(Entity receiver, DamageSource source, float amount,
                                                     Operation<Boolean> original, Player attacker,
                                                     Entity target, ItemStack stack) {
        return MalumSoulhunter.withWeapon(target, source, stack, () -> original.call(receiver, source, amount));
    }
}
