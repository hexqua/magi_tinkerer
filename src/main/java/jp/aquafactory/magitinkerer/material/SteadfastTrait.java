package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.property.CalculableToolProperty;
import net.stln.magitech.feature.tool.property.ToolProperties;
import net.stln.magitech.feature.tool.property.ToolPropertyCategory;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.property.modifier.CrossRefToolPropertyModifier;
import net.stln.magitech.feature.tool.property.modifier.ToolPropertyModifier;
import net.stln.magitech.feature.tool.property.modifier.ValueToolPropertyModifier;
import net.stln.magitech.feature.tool.trait.Trait;

import java.awt.Color;
import java.util.List;

final class SteadfastTrait extends Trait {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "steadfast");
    private static final double MIN_DURABILITY_RATIO = 1.5;
    private static final int BONUS_LIMIT_PERCENT = 40;
    private static final double BONUS_CURVE_OFFSET = 0.6;
    private static final List<ToolPropertyCategory> BOOSTED_CATEGORIES = List.of(
            ToolPropertyCategory.ATTACK, ToolPropertyCategory.ELEMENT, ToolPropertyCategory.RANGE,
            ToolPropertyCategory.DEFENSE);

    @Override
    public List<ToolPropertyModifier> modifyProperty(Player player, Level level, ItemStack stack, int traitLevel,
                                                     ToolProperties properties) {
        if (!(stack.getItem() instanceof SynthesisedToolItem tool)) {
            return List.of();
        }
        double baseDurability = tool.getToolType().defaultProperties().get().getOrId(ToolPropertyInit.DURABILITY.get());
        int durabilityDivisor = CoatedGlassTrait.durabilityDivisor(stack);
        int displayedDurability = stack.getMaxDamage();
        return BOOSTED_CATEGORIES.stream()
                .<ToolPropertyModifier>map(category -> new DurabilityBonus(category, properties, baseDurability,
                        durabilityDivisor, displayedDurability))
                .toList();
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public Color getColor() {
        return new Color(0x9A8CBD);
    }

    @Override
    public ResourceLocation getKey() {
        return ID;
    }

    private static float bonusRatio(int maxDurability, double baseDurability) {
        double excess = Math.max(0.0, maxDurability / baseDurability - MIN_DURABILITY_RATIO);
        return (float) (BONUS_LIMIT_PERCENT / 100.0 * excess / (excess + BONUS_CURVE_OFFSET));
    }

    private static final class DurabilityBonus extends ValueToolPropertyModifier<ToolProperties>
            implements CrossRefToolPropertyModifier {
        private final double baseDurability;
        private final int durabilityDivisor;
        private final int displayedDurability;

        private DurabilityBonus(ToolPropertyCategory category, ToolProperties properties, double baseDurability,
                                int durabilityDivisor, int displayedDurability) {
            super(category, properties);
            this.baseDurability = baseDurability;
            this.durabilityDivisor = durabilityDivisor;
            this.displayedDurability = displayedDurability;
        }

        @Override
        protected <T> void applyValue(ToolProperties base, CalculableToolProperty<T> property, ToolProperties target) {
            // 後段補正は共通の中間値を受けるため、空洞・集約の補正を参照値にも反映する。
            double durability = base.getOrId(ToolPropertyInit.DURABILITY.get());
            int maxDurability = (int) (durability + CoatedGlassTrait.durabilityAdjustment(durability, durabilityDivisor));
            float bonus = bonusRatio(maxDurability, baseDurability);
            // 性能加算の基準は通常の割合補正と同じ、特殊能力適用前の値を使う。
            target.set(property, property.scalarMul(value.getOrId(property), bonus));
        }

        @Override
        public MutableComponent getDisplayText() {
            // Magitech は詳細ツールチップを組み立てる前に ItemStack の最大耐久を更新する。
            float bonus = bonusRatio(displayedDurability, baseDurability);
            int roundedPercent = Math.round(bonus * 100);
            String percent = bonus > 0 && bonus < 0.01F ? "<1%" : roundedPercent + "%";
            int color = propertyCategory.getColor().getRGB();
            var label = propertyCategory.getDisplayText();
            var prefix = Component.literal(" +[").withColor(color);
            var reference = ToolPropertyCategory.DURABILITY.getDisplayText();
            var suffix = Component.literal(" → " + percent + "]").withColor(color);
            if (!enabled() || bonus == 0) {
                for (var part : List.of(label, prefix, reference, suffix)) {
                    part.withColor(0x808080).withStyle(ChatFormatting.STRIKETHROUGH);
                }
            }
            return Component.empty().append(label).append(prefix).append(reference).append(suffix);
        }
    }
}
