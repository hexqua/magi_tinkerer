package jp.aquafactory.magitinkerer.magicitem;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.magicitem.client.TravelItemClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;

public final class TravelEffects {
    private TravelEffects() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Charge.TYPE, Charge.CODEC, (payload, context) -> TravelItemClient.updateCharge(payload));
        registrar.playToClient(Flash.TYPE, Flash.CODEC, (payload, context) -> TravelItemClient.flash(payload));
    }

    static void begin(ServerPlayer player, TravelItem item, int duration) {
        item.spell().getConfig().chargeSound().ifPresent(sound -> player.level().playSound(
                null, player.getX(), player.getY(), player.getZ(), sound.get(), SoundSource.PLAYERS, 1, 1));
        sendCharge(player, duration);
    }

    static void sendCharge(ServerPlayer player, int ticks) {
        if (!player.isFakePlayer() && player.connection.hasChannel(Charge.TYPE)) {
            PacketDistributor.sendToPlayer(player, new Charge(ticks));
        }
    }

    static void complete(ServerPlayer player, TravelItem item, ServerLevel origin, Vec3 from) {
        playFlash(origin, item, from);
        if (origin != player.serverLevel() || from.distanceToSqr(player.position()) > 0.01) {
            playFlash(player.serverLevel(), item, player.position());
        }
    }

    private static void playFlash(ServerLevel level, TravelItem item, Vec3 pos) {
        item.spell().getConfig().endSound().ifPresent(sound -> level.playSound(
                null, pos.x, pos.y, pos.z, sound.get(), SoundSource.PLAYERS, 1, 1));
        Flash payload = new Flash(level.dimension().location(), pos, item.isMirror());
        for (ServerPlayer viewer : level.players()) {
            if (!viewer.isFakePlayer() && viewer.distanceToSqr(pos) <= 48 * 48 && viewer.connection.hasChannel(Flash.TYPE)) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    public record Charge(int ticks) implements CustomPacketPayload {
        private static final Type<Charge> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                MagiTinkerer.MOD_ID, "travel_charge"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Charge> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Charge::ticks, Charge::new);

        @Override
        public @NotNull Type<Charge> type() {
            return TYPE;
        }
    }

    public record Flash(ResourceLocation dimension, Vec3 pos, boolean mirror) implements CustomPacketPayload {
        private static final Type<Flash> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
                MagiTinkerer.MOD_ID, "travel_flash"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> POSITION_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, Flash> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Flash::dimension, POSITION_CODEC, Flash::pos,
                ByteBufCodecs.BOOL, Flash::mirror, Flash::new);

        @Override
        public @NotNull Type<Flash> type() {
            return TYPE;
        }
    }
}
