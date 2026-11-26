package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.magicitem.TravelItemContent;
import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.stln.magitech.content.block.BlockInit;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.recipe.InfusionRecipe;

import java.util.List;

final class InfusionAltarRecipes {
    private InfusionAltarRecipes() {
    }

    static void buildRecipes(RecipeOutput output) {
        output.accept(
                TravelItemContent.SIGNUM_THREAD.getId().withPrefix("infusion/"),
                new InfusionRecipe(
                        "",
                        SizedIngredient.of(Items.STRING, 4),
                        List.of(
                                SizedIngredient.of(ItemInit.PHANTOM_CRYSTAL.get(), 2),
                                SizedIngredient.of(ItemInit.HIGH_PURITY_FLUORITE.get(), 2)
                        ),
                        40_000,
                        TravelItemContent.SIGNUM_THREAD.toStack(1)
                ),
                null
        );

        output.accept(
                TravelItemContent.SPATIAL_MIRROR.getId().withPrefix("infusion/"),
                new InfusionRecipe(
                        "",
                        SizedIngredient.of(Items.STICK, 1),
                        List.of(
                                SizedIngredient.of(MaterialContent.DURUSIUM_INGOT.get(), 4),
                                SizedIngredient.of(ItemInit.HOLLOW_CRYSTAL.get(), 8),
                                SizedIngredient.of(ItemInit.HIGH_PURITY_FLUORITE.get(), 2),
                                SizedIngredient.of(BlockInit.MANA_INSULATING_GLASS.get(), 1)
                        ),
                        40_000,
                        TravelItemContent.SPATIAL_MIRROR.toStack(1)
                ),
                null
        );

        output.accept(
                TravelItemContent.DIMENSIONAL_MIRROR.getId().withPrefix("infusion/"),
                new InfusionRecipe(
                        "",
                        SizedIngredient.of(TravelItemContent.SPATIAL_MIRROR.get(), 1),
                        List.of(
                                SizedIngredient.of(Blocks.LODESTONE, 1),
                                SizedIngredient.of(ItemInit.HOLLOW_CRYSTAL.get(), 8)
                        ),
                        500_000,
                        TravelItemContent.DIMENSIONAL_MIRROR.toStack(1)
                ),
                null
        );
    }
}
