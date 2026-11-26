package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.machine.MenderContent;
import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.stln.magitech.content.block.BlockInit;
import net.stln.magitech.content.item.ItemInit;

final class VanillaRecipes {
    private VanillaRecipes() {
    }

    static void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MenderContent.MENDER, 1)
                .pattern("GNG")
                .pattern("DFD")
                .pattern("IAI")
                .define('D', MaterialContent.DURUSIUM_INGOT.get())
                .define('I', ItemInit.FLUXIUM_NUGGET.get())
                .define('F', BlockInit.FLUXIUM_ENCLOSURE.get())
                .define('A', BlockInit.ALCHECRYSITE.get())
                .define('N', BlockInit.ENHANCED_MANA_NODE.get())
                .define('G', BlockInit.MANA_INSULATING_GLASS.get())
                .unlockedBy("has_durusium", InventoryChangeTrigger.TriggerInstance.hasItems(MaterialContent.DURUSIUM_INGOT.get()))
                .save(output);

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
