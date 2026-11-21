package jp.aquafactory.magitinkerer.datagen;

import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.stln.magitech.content.block.BlockInit;
import net.stln.magitech.content.fluid.FluidInit;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.recipe.ZardiusCrucibleRecipe;

import java.util.List;
import java.util.Optional;

final class ZardiusCrucibleRecipes {
    private ZardiusCrucibleRecipes() {
    }

    static void buildRecipes(RecipeOutput output) {
        output.accept(
                MaterialContent.DURUSIUM_INGOT.getId().withPrefix("zardius_crucible/"),
                new ZardiusCrucibleRecipe(
                        "",
                        List.of(
                                SizedIngredient.of(ItemInit.FLUXIUM_INGOT.get(), 2),
                                SizedIngredient.of(ItemInit.HIGH_PURITY_FLUORITE.get(), 4),
                                SizedIngredient.of(ItemInit.MAGIC_CRYSTAL.get(), 4),
                                SizedIngredient.of(BlockInit.ALCHECRYSITE.get(), 1)
                        ),
                        SizedFluidIngredient.of(FluidInit.MANA_POTION.get(), 250),
                        40_000,
                        Optional.of(MaterialContent.DURUSIUM_INGOT.toStack(2)),
                        Optional.empty()
                ),
                null
        );

        output.accept(
                MaterialContent.MANEUVERING_COATED_GLASS.getId().withPrefix("zardius_crucible/"),
                new ZardiusCrucibleRecipe(
                        "",
                        List.of(
                                SizedIngredient.of(BlockInit.MANA_INSULATING_GLASS.get(), 4),
                                SizedIngredient.of(ItemInit.TREMOR_CRYSTAL.get(), 1),
                                SizedIngredient.of(BlockInit.ALCHECRYSITE.get(), 1)
                        ),
                        SizedFluidIngredient.of(FluidInit.MANA_POTION.get(), 250),
                        10_000,
                        Optional.of(MaterialContent.MANEUVERING_COATED_GLASS.toStack(4)),
                        Optional.empty()
                ),
                null
        );

        output.accept(
                MaterialContent.RESTRAINT_COATED_GLASS.getId().withPrefix("zardius_crucible/"),
                new ZardiusCrucibleRecipe(
                        "",
                        List.of(
                                SizedIngredient.of(BlockInit.MANA_INSULATING_GLASS.get(), 4),
                                SizedIngredient.of(ItemInit.RESTRAINT_QUARTZ.get(), 1),
                                SizedIngredient.of(BlockInit.ALCHECRYSITE.get(), 1)
                        ),
                        SizedFluidIngredient.of(FluidInit.MANA_POTION.get(), 250),
                        10_000,
                        Optional.of(MaterialContent.RESTRAINT_COATED_GLASS.toStack(4)),
                        Optional.empty()
                ),
                null
        );
    }
}
