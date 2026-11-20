package jp.aquafactory.magitinkerer.compat.bettercombat.network;

import jp.aquafactory.magitinkerer.compat.bettercombat.client.ClientCombat;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.CombatServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CombatNetwork {
    private CombatNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(AttackRequest.TYPE, AttackRequest.CODEC, (request, context) ->
                context.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    CombatServer.attack(player, request).ifPresentOrElse(effects ->
                            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, effects),
                            () -> PacketDistributor.sendToPlayer(player,
                                    new AttackRejected(request.sequence(), CombatServer.nextCombo(player))));
                }));
        registrar.playToClient(AttackEffects.TYPE, AttackEffects.CODEC, (effects, context) ->
                context.enqueueWork(() -> ClientCombat.show(effects)));
        registrar.playToClient(AttackRejected.TYPE, AttackRejected.CODEC, (rejected, context) ->
                context.enqueueWork(() -> ClientCombat.rejected(rejected)));
    }
}

