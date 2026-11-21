package jp.aquafactory.magitinkerer.gametest;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.property.CalculableToolProperty;
import net.stln.magitech.feature.tool.property.ToolProperties;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;
import net.stln.magitech.feature.tool.trait.Trait;
import net.stln.magitech.feature.tool.trait.TraitHelper;

import java.util.List;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class CoatedMaterialGameTests {
    @GameTest(template = "gametest/runtime")
    public static void coatedGlassSharesOneDurabilityPenaltyAfterFragileAndDurance(GameTestHelper helper) {
        var wand = SynthesisedToolGenerator.generateTool(ToolTypeInit.WAND.get(), MaterialInit.GLASS.get(),
                MaterialInit.IRON.get(), ToolMaterials.MANEUVERING_COATED_GLASS.get(),
                ToolMaterials.RESTRAINT_COATED_GLASS.get());
        var item = (SynthesisedToolItem) wand.getItem();
        var base = SynthesisedToolItem.getUpgradedProperties(wand);
        var applied = item.getAppliedProperties(null, helper.getLevel(), wand);

        assertClose(helper, applied, ToolPropertyInit.DURABILITY.get(),
                base.getOrId(ToolPropertyInit.DURABILITY.get()) * 0.6 / 3,
                "The coated glass penalty should divide the durability left after Fragile and Durance");
        assertClose(helper, applied, ToolPropertyInit.CHARGE_SPEED.get(),
                base.getOrId(ToolPropertyInit.CHARGE_SPEED.get()) * 1.6,
                "Hollow should boost handling at its own level");
        assertClose(helper, applied, ToolPropertyInit.POWER.get(),
                base.getOrId(ToolPropertyInit.POWER.get()) * 1.6,
                "Intensify should boost power at its own level");
        helper.succeed();
    }

    @GameTest(template = "gametest/runtime")
    public static void steadfastUsesReducedDurabilityAndCapsItsLevel(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var durusium = ToolMaterials.DURUSIUM.get();
        var wand = SynthesisedToolGenerator.generateTool(ToolTypeInit.WAND.get(), MaterialInit.DIAMOND.get(),
                MaterialInit.IRON.get(), durusium, ToolMaterials.MANEUVERING_COATED_GLASS.get());
        var item = (SynthesisedToolItem) wand.getItem();
        var base = SynthesisedToolItem.getUpgradedProperties(wand);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        item.reloadComponent(player, level, wand);
        var applied = item.getAppliedProperties(player, level, wand);

        int expectedDurability = (int) (base.getOrId(ToolPropertyInit.DURABILITY.get()) * 1.5 / 2);
        helper.assertTrue(wand.getMaxDamage() == expectedDurability,
                "The wand should store the maximum durability after Hollow");
        var twoDurusium = SynthesisedToolGenerator.generateTool(ToolTypeInit.WAND.get(), MaterialInit.DIAMOND.get(),
                MaterialInit.DIAMOND.get(), durusium, durusium);
        helper.assertTrue(TraitHelper.getTrait(twoDurusium).stream().anyMatch(
                        instance -> instance.trait() == durusium.getTrait() && instance.level() == 1),
                "Two Durusium parts should still grant only Steadfast I");

        double excess = expectedDurability / 378.0 - 1.5;
        helper.assertTrue(excess > 0.0, "The fixture should exceed the Steadfast threshold after Hollow");
        double multiplier = 1.0 + 0.40 * excess / (excess + 0.60);
        for (var property : List.of(ToolPropertyInit.POWER.get(), ToolPropertyInit.DEFENSE.get())) {
            assertClose(helper, applied, property, base.getOrId(property) * multiplier,
                    "Steadfast should scale " + property.getDisplayName().getString() + " from final durability");
        }
        assertClose(helper, applied, ToolPropertyInit.LAUNCH.get(),
                base.getOrId(ToolPropertyInit.LAUNCH.get()) * (multiplier + 0.30),
                "Steadfast and Hollow should add their range bonuses");
        double baseManaPower = base.getOrId(ToolPropertyInit.ELEMENTAL_POWER.get()).get(Element.MANA);
        double actualManaPower = applied.getOrId(ToolPropertyInit.ELEMENTAL_POWER.get()).get(Element.MANA);
        helper.assertTrue(Math.abs(actualManaPower - baseManaPower * multiplier) < 0.0001,
                "Steadfast should scale elemental power from final durability");
        String displayedBonus = Math.round((multiplier - 1) * 100) + "%]";
        assertSteadfastTooltip(helper, durusium.getTrait(), wand, applied, displayedBonus, false);

        var belowThreshold = SynthesisedToolGenerator.generateTool(ToolTypeInit.WAND.get(), MaterialInit.DIAMOND.get(),
                MaterialInit.DIAMOND.get(), durusium, ToolMaterials.MANEUVERING_COATED_GLASS.get());
        var belowBase = SynthesisedToolItem.getUpgradedProperties(belowThreshold);
        player.setItemInHand(InteractionHand.MAIN_HAND, belowThreshold);
        item.reloadComponent(player, level, belowThreshold);
        var belowApplied = item.getAppliedProperties(player, level, belowThreshold);
        assertClose(helper, belowApplied, ToolPropertyInit.POWER.get(),
                belowBase.getOrId(ToolPropertyInit.POWER.get()),
                "Steadfast should grant no power boost below the durability threshold");
        assertSteadfastTooltip(helper, durusium.getTrait(), belowThreshold, belowApplied, "0%]", true);
        helper.succeed();
    }

    private static void assertSteadfastTooltip(GameTestHelper helper, Trait trait, ItemStack stack,
                                               ToolProperties applied, String suffix, boolean inactive) {
        var modifiers = trait.modifyProperty(null, helper.getLevel(), stack, 1, applied);
        helper.assertTrue(!modifiers.isEmpty(), "Steadfast should show its bonuses in the detailed tooltip");
        for (var modifier : modifiers) {
            var line = modifier.getDisplayText();
            helper.assertTrue(line.getString().endsWith(suffix),
                    "Steadfast should show its effective bonus: " + line.getString());
            helper.assertTrue(line.getSiblings().stream().allMatch(part -> part.getStyle().isStrikethrough() == inactive),
                    "The entire Steadfast bonus line should use the correct active or inactive style");
        }
    }

    private static void assertClose(GameTestHelper helper, ToolProperties applied,
                                    CalculableToolProperty<Double> property, double expected, String message) {
        double actual = applied.getOrId(property);
        helper.assertTrue(Math.abs(actual - expected) < 0.0001,
                message + " (expected " + expected + ", got " + actual + ")");
    }
}
