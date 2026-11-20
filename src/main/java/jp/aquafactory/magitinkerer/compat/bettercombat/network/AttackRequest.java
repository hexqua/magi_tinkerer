package jp.aquafactory.magitinkerer.compat.bettercombat.network;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record AttackRequest(long sequence, int combo, int slot) implements CustomPacketPayload {
    public static final Type<AttackRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            MagiTinkerer.MOD_ID, "attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttackRequest> CODEC = StreamCodec.of(
            (buffer, request) -> {
                buffer.writeVarLong(request.sequence);
                buffer.writeVarInt(request.combo);
                buffer.writeVarInt(request.slot);
            }, buffer -> new AttackRequest(buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

