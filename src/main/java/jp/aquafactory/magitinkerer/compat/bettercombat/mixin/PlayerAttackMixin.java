package jp.aquafactory.magitinkerer.compat.bettercombat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackContext;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @SuppressWarnings("resource")
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void magitinkerer$serverRoute(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        // vanilla の攻撃 packet を併送しても、専用要求へ物理攻撃を上乗せさせない。
        if (!player.level().isClientSide && SupportedWeapons.contains(player.getMainHandItem())
                && AttackContext.current(player) == null) ci.cancel();
    }

    // 攻撃開始の判定や特性の計算には、フルチャージ補正を適用しない。
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void magitinkerer$fullCharge(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (AttackContext.fullCharge((Player) (Object) this)) cir.setReturnValue(1.0F);
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double magitinkerer$physicalDamage(Player player, Holder<Attribute> attribute,
                                                     Operation<Double> original) {
        double value = original.call(player, attribute);
        return attribute.equals(Attributes.ATTACK_DAMAGE)
                ? AttackContext.scale(player, AttackContext.Phase.PHYSICAL, value) : value;
    }
}
