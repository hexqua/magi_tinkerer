package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.ToolMaterialModels;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import java.util.concurrent.CompletableFuture;

final class ToolMaterialSpriteSourceProvider extends SpriteSourceProvider {
    ToolMaterialSpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                     ExistingFileHelper existingFiles) {
        super(output, registries, MagiTinkerer.MOD_ID, existingFiles);
    }

    @Override
    protected void gather() {
        var textures = ToolMaterialModels.models().stream()
                .map(ToolMaterialModels.Model::source)
                .distinct()
                .sorted()
                .toList();
        atlas(BLOCKS_ATLAS).addSource(new PalettedPermutations(textures, ToolMaterialModels.PALETTE_KEY,
                ToolMaterialModels.palettes()));
    }
}
