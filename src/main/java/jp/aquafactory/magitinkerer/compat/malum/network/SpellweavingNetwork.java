package jp.aquafactory.magitinkerer.compat.malum.network;

import com.sammy.malum.core.listeners.SpellweavingEqualityReloadListener;
import jp.aquafactory.magitinkerer.compat.malum.SpellweavingBlockMatch;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SpellweavingNetwork {
    private SpellweavingNetwork() {}

    public static void register(IEventBus modBus, Dist dist) {
        modBus.addListener(SpellweavingNetwork::registerPayload);
        NeoForge.EVENT_BUS.addListener(SpellweavingNetwork::syncGroups);
        if (dist == Dist.CLIENT) {
            ClientEvents.register();
        }
    }

    private static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(
                SyncSpellweavingGroupsPayload.TYPE, SyncSpellweavingGroupsPayload.CODEC,
                (payload, context) -> SpellweavingBlockMatch.updatePreviewGroups(payload.groups()));
    }

    private static void syncGroups(OnDatapackSyncEvent event) {
        var recipients = event.getRelevantPlayers()
                .filter(player -> player.connection.hasChannel(SyncSpellweavingGroupsPayload.TYPE))
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        var groups = SpellweavingEqualityReloadListener.EQUALITY_DATA.stream()
                .map(group -> group.equalBlocks().stream()
                        .map(Holder::value)
                        .map(BuiltInRegistries.BLOCK::getKey)
                        .toList())
                .toList();
        var payload = new SyncSpellweavingGroupsPayload(groups);
        recipients.forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static final class ClientEvents {
        private static void register() {
            NeoForge.EVENT_BUS.addListener(ClientEvents::onLogout);
        }

        private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            SpellweavingBlockMatch.clearPreviewGroups();
        }
    }
}
