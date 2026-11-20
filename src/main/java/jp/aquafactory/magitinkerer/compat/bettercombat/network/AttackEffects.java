package jp.aquafactory.magitinkerer.compat.bettercombat.network;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SlashVisual;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record AttackEffects(int playerId, boolean offHand, ItemStack weapon, int[] targets,
                            Optional<SlashVisual> visual)
        implements CustomPacketPayload {
    public static final Type<AttackEffects> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            MagiTinkerer.MOD_ID, "attack_effects"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttackEffects> CODEC = StreamCodec.of(
            (buffer, effects) -> {
                buffer.writeVarInt(effects.playerId);
                buffer.writeBoolean(effects.offHand);
                ItemStack.STREAM_CODEC.encode(buffer, effects.weapon);
                buffer.writeVarIntArray(effects.targets);
                buffer.writeOptional(effects.visual, (output, value) -> {
                    writeVector(output, value.center());
                    writeVector(output, value.right());
                    writeVector(output, value.up());
                    writeVector(output, value.forward());
                    output.writeDouble(value.halfWidth());
                    output.writeDouble(value.halfHeight());
                    output.writeDouble(value.halfDepth());
                    output.writeCollection(value.rolls(), FriendlyByteBuf::writeFloat);
                    output.writeInt(value.primary());
                    output.writeInt(value.secondary());
                });
            }, buffer -> new AttackEffects(buffer.readVarInt(), buffer.readBoolean(),
                    ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarIntArray(),
                    buffer.readOptional(input -> new SlashVisual(readVector(input), readVector(input),
                            readVector(input), readVector(input), input.readDouble(), input.readDouble(),
                            input.readDouble(), input.readList(FriendlyByteBuf::readFloat),
                            input.readInt(), input.readInt()))));

    private static void writeVector(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

