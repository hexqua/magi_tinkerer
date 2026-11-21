package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

final class MaterialBlockLoot extends BlockLootSubProvider {
    MaterialBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(this::dropSelf);
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return List.of(MaterialContent.MANEUVERING_COATED_GLASS.get(), MaterialContent.MANEUVERING_COATED_GLASS_PANE.get(),
                MaterialContent.RESTRAINT_COATED_GLASS.get(), MaterialContent.RESTRAINT_COATED_GLASS_PANE.get());
    }
}
