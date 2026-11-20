package jp.aquafactory.magitinkerer.compat.bettercombat.mixin.client;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class ClientPredictionMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void magitinkerer$skipPrediction(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player == Minecraft.getInstance().player && SupportedWeapons.currentAttack(player)) ci.cancel();
    }
}
