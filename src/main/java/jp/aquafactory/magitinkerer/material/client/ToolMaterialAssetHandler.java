package jp.aquafactory.magitinkerer.material.client;

import io.redspace.atlasapi.api.AssetHandler;
import io.redspace.atlasapi.api.AtlasApiRegistry;
import io.redspace.atlasapi.api.data.BakingPreparations;
import jp.aquafactory.magitinkerer.material.ToolMaterialModels;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ToolMaterialAssetHandler extends AssetHandler {
    public static void register(IEventBus modBus) {
        var handlers = DeferredRegister.create(AtlasApiRegistry.ASSET_HANDLER_REGISTRY_KEY,
                ToolMaterialModels.HANDLER.getNamespace());
        handlers.register(ToolMaterialModels.HANDLER.getPath(), ToolMaterialAssetHandler::new);
        handlers.register(modBus);
    }

    @Override
    public @NotNull List<SpriteSource> buildSpriteSources() {
        var textures = ToolMaterialModels.prismarineModels().stream().map(ToolMaterialModels.Model::source).toList();
        return List.of(new PalettedPermutations(textures, ToolMaterialModels.PALETTE_KEY,
                Map.of(ToolMaterialModels.PRISMARINE_PERMUTATION, ToolMaterialModels.PRISMARINE_PALETTE)));
    }

    // simple_model は JSON の固定スプライトを使う。
    @Override
    public @NotNull BakingPreparations makeBakedModelPreparations(ItemStack stack, @Nullable ClientLevel level,
                                                                 @Nullable LivingEntity entity, int seed) {
        return BakingPreparations.empty();
    }

    @Override
    public int modelId(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return 0;
    }
}
