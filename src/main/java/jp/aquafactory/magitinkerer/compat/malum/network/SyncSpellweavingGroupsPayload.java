package jp.aquafactory.magitinkerer.compat.malum.network;

import io.netty.buffer.ByteBuf;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

record SyncSpellweavingGroupsPayload(List<List<ResourceLocation>> groups) implements CustomPacketPayload {
    static final Type<SyncSpellweavingGroupsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "sync_spellweaving_groups"));
    static final StreamCodec<ByteBuf, SyncSpellweavingGroupsPayload> CODEC = ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .apply(ByteBufCodecs.list())
            .map(SyncSpellweavingGroupsPayload::new, SyncSpellweavingGroupsPayload::groups);

    @Override
    public @NotNull Type<SyncSpellweavingGroupsPayload> type() {
        return TYPE;
    }
}
