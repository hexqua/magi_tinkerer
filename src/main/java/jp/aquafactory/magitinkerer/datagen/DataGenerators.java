package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.MaterialContent;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.stln.magitech.content.recipe.ToolMaterialRecipe;
import net.stln.magitech.feature.tool.material.ToolMaterial;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class DataGenerators {
    private DataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new MagiTinkererRecipes(output, event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new MaterialTags(output, event.getLookupProvider(), event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(MaterialBlockLoot::new, LootContextParamSets.BLOCK)),
                event.getLookupProvider()));
        generator.addProvider(event.includeClient(), new ToolMaterialModelProvider(output));
        generator.addProvider(event.includeClient(), new ToolMaterialSpriteSourceProvider(output, event.getLookupProvider(),
                event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new MaterialModelProvider(output, event.getExistingFileHelper()));
    }

    private static final class MagiTinkererRecipes extends RecipeProvider {
        private MagiTinkererRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(@NotNull RecipeOutput output) {
            VanillaRecipes.buildRecipes(output);
            ZardiusCrucibleRecipes.buildRecipes(output);
            buildMaterialRecipes(output);
        }

        private static void buildMaterialRecipes(RecipeOutput output) {
            materialRecipe(output, Ingredient.of(Items.PRISMARINE_CRYSTALS), ToolMaterials.PRISMARINE_CRYSTAL.get());
            materialRecipe(output, Ingredient.of(MaterialContent.MANEUVERING_COATED_GLASS.get()),
                    ToolMaterials.MANEUVERING_COATED_GLASS.get());
            materialRecipe(output, Ingredient.of(MaterialContent.RESTRAINT_COATED_GLASS.get()),
                    ToolMaterials.RESTRAINT_COATED_GLASS.get());
            materialRecipe(output, Ingredient.of(MaterialContent.DURUSIUM_INGOT.get()), ToolMaterials.DURUSIUM.get());

            var malumOutput = output.withConditions(new ModLoadedCondition(MagiTinkerer.MALUM_MOD_ID),
                    new ItemExistsCondition(ToolMaterials.SOULSTAINED_STEEL_INGOT));
            materialRecipe(malumOutput, Ingredient.of(ToolMaterials.SOULSTAINED_STEEL_INGOTS),
                    ToolMaterials.SOULSTAINED_STEEL.get());
        }

        private static void materialRecipe(RecipeOutput output, Ingredient ingredient, ToolMaterial material) {
            output.accept(material.getId().withPrefix("part_material/"),
                    new ToolMaterialRecipe("", ingredient, material), null);
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
