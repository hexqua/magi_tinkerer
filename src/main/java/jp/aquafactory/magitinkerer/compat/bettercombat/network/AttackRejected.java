package jp.aquafactory.magitinkerer.compat.bettercombat.network;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record AttackRejected(long sequence, int nextCombo) implements CustomPacketPayload {
    public static final Type<AttackRejected> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            MagiTinkerer.MOD_ID, "attack_rejected"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttackRejected> CODEC = StreamCodec.of(
            (buffer, rejected) -> {
                buffer.writeVarLong(rejected.sequence);
                buffer.writeVarInt(rejected.nextCombo);
            }, buffer -> new AttackRejected(buffer.readVarLong(), buffer.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
