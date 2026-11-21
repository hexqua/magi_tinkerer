package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.stln.magitech.feature.tool.material.ToolMaterial;
import net.stln.magitech.feature.tool.property.ToolProperties;
import net.stln.magitech.feature.tool.property.ToolPropertyCategory;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.property.modifier.CrossRefToolPropertyModifier;
import net.stln.magitech.feature.tool.property.modifier.RationalToolPropertyModifier;
import net.stln.magitech.feature.tool.property.modifier.ToolPropertyModifier;
import net.stln.magitech.feature.tool.trait.Trait;
import net.stln.magitech.helper.ComponentHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class CoatedGlassTrait extends Trait {
    private final ResourceLocation id;
    private final Color color;
    private final List<ToolPropertyCategory> boostedCategories;

    CoatedGlassTrait(String path, Color color, ToolPropertyCategory... boostedCategories) {
        this.id = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, path);
        this.color = color;
        this.boostedCategories = List.of(boostedCategories);
    }

    @Override
    public List<ToolPropertyModifier> modifyProperty(Player player, Level level, ItemStack stack, int traitLevel,
                                                     ToolProperties properties) {
        List<ToolPropertyModifier> modifiers = new ArrayList<>();
        for (var category : boostedCategories) {
            modifiers.add(new RationalToolPropertyModifier(category, 0.30F * traitLevel));
        }
        var glassTraits = coatedGlassTraits(stack);
        if (!glassTraits.isEmpty() && glassTraits.getFirst() == this) {
            modifiers.add(new DurabilityModifier(1 + glassTraits.size()));
        }
        return modifiers;
    }

    static int durabilityDivisor(ItemStack stack) {
        return 1 + coatedGlassTraits(stack).size();
    }

    private static List<Trait> coatedGlassTraits(ItemStack stack) {
        return ComponentHelper.getPartMaterials(stack).stream()
                .map(ToolMaterial::getTrait)
                .filter(CoatedGlassTrait.class::isInstance)
                .toList();
    }

    static double durabilityAdjustment(double durability, int divisor) {
        return durability / divisor - durability;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public ResourceLocation getKey() {
        return id;
    }

    // 共通の減算を一度だけ適用し、最大耐久の整数化までは double の精度を維持する。
    private static final class DurabilityModifier extends RationalToolPropertyModifier
            implements CrossRefToolPropertyModifier {
        private final int divisor;

        private DurabilityModifier(int divisor) {
            super(ToolPropertyCategory.DURABILITY, 1.0F / divisor - 1.0F);
            this.divisor = divisor;
        }

        @Override
        public ToolProperties apply(ToolProperties base) {
            var adjustment = new ToolProperties(base.getCategory());
            var durability = ToolPropertyInit.DURABILITY.get();
            if (enabled() && base.getValues().containsKey(durability)) {
                adjustment.set(durability, durabilityAdjustment(base.getOrId(durability), divisor));
            }
            return adjustment;
        }
    }
}
