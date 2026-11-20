package jp.aquafactory.magitinkerer.compat.bettercombat.mixin;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.CombatServer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerNetwork.class, remap = false)
public abstract class ServerAttackMixin {
    @Inject(method = "handleAttackRequest", at = @At("HEAD"), cancellable = true)
    private static void magitinkerer$routeAttack(Packets.C2S_AttackRequest request,
                                                       MinecraftServer server, ServerPlayer player,
                                                       ServerGamePacketListenerImpl handler, CallbackInfo ci) {
        if (request.comboCount() < 0) {
            if (SupportedWeapons.contains(player.getMainHandItem())
                    || SupportedWeapons.contains(player.getOffhandItem())) ci.cancel();
            return;
        }
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, request.comboCount());
        if (hand != null && SupportedWeapons.contains(hand.itemStack())) ci.cancel();
        else CombatServer.observeOrdinaryAttack(player, request.comboCount(), hand);
    }
}
