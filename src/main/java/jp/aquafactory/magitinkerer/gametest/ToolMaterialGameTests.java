package jp.aquafactory.magitinkerer.gametest;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.component.MaterialComponent;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.recipe.RecipeInit;
import net.stln.magitech.content.recipe.input.MultiStackRecipeInput;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;
import net.stln.magitech.feature.tool.trait.TraitHelper;
import net.stln.magitech.helper.ComponentHelper;

import java.util.List;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class ToolMaterialGameTests {
    @GameTest(template = "gametest/runtime")
    public static void prismarinePartKeepsItsMaterialThroughToolAssembly(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getRecipeManager();
        var registries = level.registryAccess();
        var prismarine = ToolMaterials.PRISMARINE_CRYSTAL.get();
        var iron = MaterialInit.IRON.get();

        var cuttingInput = new SingleRecipeInput(new ItemStack(Items.PRISMARINE_CRYSTALS, 2));
        var cuttingRecipe = recipes.getRecipesFor(RecipeInit.PART_CUTTING_TYPE.get(), cuttingInput, level).stream()
                .filter(recipe -> recipe.value().getResultItem(registries).is(ItemInit.SPIKE_HEAD.get()))
                .findFirst();
        helper.assertTrue(cuttingRecipe.isPresent(), "The workbench did not offer a spike head for prismarine crystals");
        var spikeHead = cuttingRecipe.orElseThrow().value().assemble(cuttingInput, registries);
        helper.assertTrue(ComponentHelper.getMaterial(spikeHead).orElse(null) == prismarine,
                "The cut spike head did not receive the prismarine crystal material");

        var handle = ItemInit.HEAVY_HANDLE.toStack();
        handle.set(ComponentInit.MATERIAL_COMPONENT.get(), new MaterialComponent(iron));
        var binding = ItemInit.TOOL_BINDING.toStack();
        binding.set(ComponentInit.MATERIAL_COMPONENT.get(), new MaterialComponent(iron));
        var assemblyInput = new MultiStackRecipeInput(List.of(handle, spikeHead, binding));
        var assemblyRecipe = recipes.getRecipeFor(RecipeInit.TOOL_ASSEMBLY_TYPE.get(), assemblyInput, level);
        helper.assertTrue(assemblyRecipe.isPresent(), "The assembly workbench did not offer a tool for the mixed parts");
        var tool = assemblyRecipe.orElseThrow().value().assemble(assemblyInput, registries);

        helper.assertTrue(tool.is(ItemInit.PICKAXE.get()), "The mixed parts did not assemble into a pickaxe");
        helper.assertTrue(ComponentHelper.getPartMaterials(tool).equals(List.of(iron, prismarine, iron)),
                "The assembled pickaxe did not preserve each part's material");
        helper.assertTrue(TraitHelper.getTrait(tool).stream().anyMatch(instance -> instance.trait() == prismarine.getTrait()),
                "The assembled pickaxe did not inherit the Sea Friends trait");
        helper.succeed();
    }

    @GameTest(template = "gametest/runtime")
    public static void soulstainedSteelRecipeIsOptionalButEquipmentRemainsReadable(GameTestHelper helper) {
        var level = helper.getLevel();
        var steel = ToolMaterials.SOULSTAINED_STEEL.get();
        var recipeId = steel.getId().withPrefix("part_material/");
        boolean malumLoaded = ModList.get().isLoaded(MagiTinkerer.MALUM_MOD_ID);
        helper.assertTrue(level.getRecipeManager().byKey(recipeId).isPresent() == malumLoaded,
                "The soulstained steel material recipe should follow Malum availability");
        if (malumLoaded) {
            var ingot = BuiltInRegistries.ITEM.get(ToolMaterials.SOULSTAINED_STEEL_INGOT);
            var cuttingInput = new SingleRecipeInput(new ItemStack(ingot, 2));
            var recipe = level.getRecipeManager().getRecipesFor(RecipeInit.PART_CUTTING_TYPE.get(), cuttingInput, level)
                    .stream().filter(entry -> entry.value().getResultItem(level.registryAccess()).is(ItemInit.SPIKE_HEAD.get()))
                    .findFirst();
            helper.assertTrue(recipe.isPresent(), "The workbench did not offer a spike head for Soulstained Steel Ingots");
            var part = recipe.orElseThrow().value().assemble(cuttingInput, level.registryAccess());
            helper.assertTrue(ComponentHelper.getMaterial(part).orElse(null) == steel,
                    "The cut spike head did not receive the soulstained steel material");
        }

        var tool = SynthesisedToolGenerator.generateTool(ToolTypeInit.PICKAXE.get(),
                MaterialInit.IRON.get(), steel, MaterialInit.IRON.get());
        var registries = level.registryAccess();
        var loaded = ItemStack.parse(registries, tool.save(registries)).orElseThrow();
        helper.assertTrue(ComponentHelper.getPartMaterials(loaded).equals(ComponentHelper.getPartMaterials(tool)),
                "The tool did not retain its mixed materials after saving and loading");
        helper.succeed();
    }

    @GameTest(template = "gametest/runtime")
    public static void soulhunterUsesTheCastingWandAndAllowsSpiritPlunderThree(GameTestHelper helper) {
        if (ModList.get().isLoaded(MagiTinkerer.MALUM_MOD_ID)) {
            MalumSoulhunterGameTest.verify(helper);
        }
        helper.succeed();
    }
}
