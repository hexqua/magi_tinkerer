package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.stln.magitech.content.recipe.ToolMaterialRecipe;

import java.util.concurrent.CompletableFuture;

public final class DataGenerators {
    private DataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new MaterialRecipes(output, event.getLookupProvider()));
        generator.addProvider(event.includeClient(), new ToolMaterialModelProvider(output));
    }

    private static final class MaterialRecipes extends RecipeProvider {
        private MaterialRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            var material = ToolMaterials.PRISMARINE_CRYSTAL.get();
            output.accept(material.getId().withPrefix("part_material/"),
                    new ToolMaterialRecipe("", Ingredient.of(Items.PRISMARINE_CRYSTALS), material), null);
        }
    }
}
