package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;

final class VanillaRecipes {
    private VanillaRecipes() {
    }

    static void buildRecipes(RecipeOutput output) {
        glassPane(output, MaterialContent.MANEUVERING_COATED_GLASS.get(),
                MaterialContent.MANEUVERING_COATED_GLASS_PANE.get());
        glassPane(output, MaterialContent.RESTRAINT_COATED_GLASS.get(),
                MaterialContent.RESTRAINT_COATED_GLASS_PANE.get());
    }

    private static void glassPane(RecipeOutput output, ItemLike glass, ItemLike pane) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pane, 16)
                .define('#', glass)
                .pattern("###")
                .pattern("###")
                .unlockedBy("has_glass", InventoryChangeTrigger.TriggerInstance.hasItems(glass))
                .save(output);
    }
}
