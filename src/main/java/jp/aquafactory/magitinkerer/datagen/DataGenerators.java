package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.stln.magitech.content.recipe.ToolMaterialRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class DataGenerators {
    private DataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new MaterialRecipes(output, event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new MaterialTags(output, event.getLookupProvider(), event.getExistingFileHelper()));
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

            var steel = ToolMaterials.SOULSTAINED_STEEL.get();
            output.withConditions(new ModLoadedCondition(MagiTinkerer.MALUM_MOD_ID),
                            new ItemExistsCondition(ToolMaterials.SOULSTAINED_STEEL_INGOT))
                    .accept(steel.getId().withPrefix("part_material/"),
                            new ToolMaterialRecipe("", Ingredient.of(ToolMaterials.SOULSTAINED_STEEL_INGOTS), steel), null);
        }
    }

    private static final class MaterialTags extends TagsProvider<Item> {
        private MaterialTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                             ExistingFileHelper existingFiles) {
            super(output, Registries.ITEM, registries, MagiTinkerer.MOD_ID, existingFiles);
        }

        @Override
        protected void addTags(@NotNull HolderLookup.Provider registries) {
            tag(ToolMaterials.SOULSTAINED_STEEL_INGOTS).addOptional(ToolMaterials.SOULSTAINED_STEEL_INGOT);
        }
    }
}
