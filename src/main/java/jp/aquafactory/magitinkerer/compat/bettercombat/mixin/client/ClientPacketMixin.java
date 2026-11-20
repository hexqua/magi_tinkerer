package jp.aquafactory.magitinkerer.compat.bettercombat.mixin.client;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.neoforge.PlatformImpl;
import net.bettercombat.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlatformImpl.class, remap = false)
public abstract class ClientPacketMixin {
    // モーション同期や空振り時のブロック効果 packet は変更しない。
    @Inject(method = "networkC2S_Send", at = @At("HEAD"), cancellable = true)
    private static void magitinkerer$skipNormalRequest(CustomPacketPayload payload, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && payload instanceof Packets.C2S_AttackRequest request && request.comboCount() >= 0) {
            var hand = PlayerAttackHelper.getCurrentAttack(player, request.comboCount());
            if (hand != null && SupportedWeapons.contains(hand.itemStack())) ci.cancel();
        }
    }
}
