package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

final class MaterialModelProvider extends BlockStateProvider {
    MaterialModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, MagiTinkerer.MOD_ID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        glass(MaterialContent.MANEUVERING_COATED_GLASS.get(), MaterialContent.MANEUVERING_COATED_GLASS_PANE.get());
        glass(MaterialContent.RESTRAINT_COATED_GLASS.get(), MaterialContent.RESTRAINT_COATED_GLASS_PANE.get());
        itemModels().basicItem(MaterialContent.DURUSIUM_INGOT.get());
    }

    private void glass(Block block, IronBarsBlock pane) {
        var texture = blockTexture(block);
        simpleBlockWithItem(block, models().cubeAll(BuiltInRegistries.BLOCK.getKey(block).getPath(), texture)
                .renderType("translucent"));
        paneBlockWithRenderType(pane, texture, texture.withSuffix("_pane_top"), "translucent");
        itemModels().getBuilder(BuiltInRegistries.BLOCK.getKey(pane).getPath())
                .parent(itemModels().getExistingFile(mcLoc("item/generated")))
                .texture("layer0", texture);
    }
}
