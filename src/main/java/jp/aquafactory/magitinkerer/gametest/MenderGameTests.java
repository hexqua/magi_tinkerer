package jp.aquafactory.magitinkerer.gametest;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.machine.ManaDurumMenderBlockEntity;
import jp.aquafactory.magitinkerer.machine.MenderContent;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.block.BlockInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;
import net.stln.magitech.helper.ComponentHelper;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class MenderGameTests {
    private static final BlockPos MENDER_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "gametest/runtime")
    public static void menderReceivesManaThroughEitherVerticalConnection(GameTestHelper helper) {
        var mender = placeMender(helper);
        helper.setBlock(MENDER_POS.above(), BlockInit.CREATIVE_MANA_SOURCE.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mender.getMana() > 0, "The top source should supply mana"))
                .thenExecute(() -> {
                    helper.setBlock(MENDER_POS.above(), Blocks.AIR);
                    mender.setMana(0);
                    helper.setBlock(MENDER_POS.below(), BlockInit.CREATIVE_MANA_SOURCE.get());
                })
                .thenWaitUntil(() -> helper.assertTrue(mender.getMana() > 0, "The bottom source should supply mana"))
                .thenSucceed();
    }

    @GameTest(template = "gametest/runtime")
    public static void menderRepairsOnlyAffordableDamageAndReleasesFinishedTool(GameTestHelper helper) {
        var mender = placeMender(helper);
        var level = helper.getLevel();
        var pos = helper.absolutePos(MENDER_POS);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(level.getCapability(ManaCapabilities.MANA_CONTAINER, pos, side) == null,
                    "Horizontal sides should not connect to mana");
        }
        for (Direction side : Direction.values()) {
            helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null,
                    "Every side should expose automation");
        }
        var above = level.getCapability(ManaCapabilities.MANA_CONTAINER, pos, Direction.UP);
        var below = level.getCapability(ManaCapabilities.MANA_CONTAINER, pos, Direction.DOWN);
        if (above == null || below == null) {
            helper.fail("Both vertical sides should connect to mana");
            return;
        }
        helper.assertTrue(above.insertMana(12_000, false) == 5_000 && below.insertMana(12_000, true) == 0,
                "Both mana inputs should share the per-tick transfer limit");
        helper.assertTrue(above.extractMana(5_000, false) == 0 && below.extractMana(5_000, false) == 0,
                "The mender should not export mana");

        var automation = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.EAST);
        if (automation == null) {
            helper.fail("The item capability should be available");
            return;
        }
        var ordinary = new ItemStack(Items.IRON_PICKAXE);
        helper.assertTrue(automation.insertItem(0, ordinary, false).getCount() == 1 && mender.isEmpty(),
                "Automation should reject tools without Durusium");
        var wand = makeWand(helper, 8);
        mender.setMana(32_000);
        helper.assertTrue(automation.insertItem(0, wand, false).isEmpty(), "Automation should accept a mixed tool");

        helper.runAfterDelay(9, () -> helper.assertTrue(mender.getItem(0).getDamageValue() == 8,
                "The tool should wait ten ticks for its first repair"));
        helper.runAfterDelay(11, () -> helper.assertTrue(mender.getItem(0).getDamageValue() == 3
                && mender.getMana() == 7_000, "The first batch should repair five points for 25 kJ"));
        helper.runAfterDelay(21, () -> helper.assertTrue(mender.getItem(0).getDamageValue() == 2
                && mender.getMana() == 2_000, "The second batch should repair only the affordable point"));
        helper.runAfterDelay(31, () -> {
            helper.assertTrue(mender.getItem(0).getDamageValue() == 2 && mender.getMana() == 2_000,
                    "The mender should wait when it cannot afford a point");
            helper.assertTrue(automation.extractItem(0, 1, true).isEmpty()
                    && automation.extractItem(0, 1, false).isEmpty(), "Automation should leave unfinished tools in place");
            helper.assertTrue(below.insertMana(5_000, false) == 5_000, "The bottom input should accept mana");
            helper.assertTrue(above.insertMana(5_000, false) == 0, "The inputs should share the limit after waiting");
        });
        helper.runAfterDelay(32, () -> above.insertMana(3_000, false));
        helper.runAfterDelay(41, () -> {
            var preview = automation.extractItem(0, 1, true);
            helper.assertTrue(!preview.isEmpty() && preview.getDamageValue() == 0 && !mender.isEmpty(),
                    "A completed tool should be available without mutation during simulation");
            var finished = automation.extractItem(0, 1, false);
            helper.assertTrue(!finished.isEmpty() && finished.getDamageValue() == 0 && mender.isEmpty()
                    && mender.getMana() == 0, "Refilling should finish the tool and allow one extraction");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/runtime")
    public static void menderPreservesToolOnSwapReloadAndBreak(GameTestHelper helper) {
        var mender = placeMender(helper);
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var first = makeWand(helper, 7);
        first.set(ComponentInit.TIER_COMPONENT, 3);
        var broken = makeWand(helper, 0);
        broken.set(ComponentInit.TIER_COMPONENT, 4);
        broken.setDamageValue(broken.getMaxDamage() - 1);
        broken.set(ComponentInit.BROKEN_COMPONENT, true);

        player.setItemInHand(InteractionHand.MAIN_HAND, first.copy());
        helper.useBlock(MENDER_POS, player);
        helper.assertTrue(player.getMainHandItem().isEmpty() && ItemStack.matches(mender.getItem(0), first),
                "Right click should put exactly one tool in the mender");
        var ordinary = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, ordinary);
        helper.useBlock(MENDER_POS, player);
        helper.assertTrue(ItemStack.matches(player.getMainHandItem(), ordinary)
                && ItemStack.matches(mender.getItem(0), first), "Rejection should keep both items unchanged");
        player.setItemInHand(InteractionHand.MAIN_HAND, broken);
        helper.useBlock(MENDER_POS, player);
        helper.assertTrue(ItemStack.matches(player.getMainHandItem(), first)
                && ItemStack.matches(mender.getItem(0), broken), "Right click should swap the two tools");

        mender.setMana(5_000);
        var saved = mender.saveWithoutMetadata(level.registryAccess());
        level.destroyBlock(helper.absolutePos(MENDER_POS), true);
        helper.assertItemEntityCountIs(broken.getItem(), MENDER_POS, 1, 1);
        helper.assertItemEntityCountIs(MenderContent.MENDER_ITEM.get(), MENDER_POS, 1, 1);
        var restored = placeMender(helper);
        helper.assertTrue(restored.getMana() == 0 && restored.isEmpty(), "A fresh block should be empty and uncharged");
        restored.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(restored.getMana() == 5_000 && ItemStack.matches(restored.getItem(0), broken),
                "Loading persisted data should restore the exact tool and mana");
        helper.runAfterDelay(11, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.useBlock(MENDER_POS, player);
            var repaired = player.getMainHandItem();
            helper.assertTrue(restored.isEmpty() && !repaired.isEmpty()
                    && repaired.getDamageValue() == broken.getDamageValue() - 1
                    && !ComponentHelper.isBroken(repaired) && restored.getMana() == 0,
                    "One paid repair point should restore the broken tool and allow manual removal");
            var expected = broken.copy();
            expected.setDamageValue(broken.getDamageValue() - 1);
            expected.set(ComponentInit.BROKEN_COMPONENT, false);
            helper.assertTrue(ItemStack.matches(repaired, expected), "A repair should preserve the other components");
            helper.succeed();
        });
    }

    private static ManaDurumMenderBlockEntity placeMender(GameTestHelper helper) {
        helper.setBlock(MENDER_POS, MenderContent.MENDER.get());
        return helper.getBlockEntity(MENDER_POS);
    }

    private static ItemStack makeWand(GameTestHelper helper, int damage) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var wand = SynthesisedToolGenerator.generateTool(ToolTypeInit.WAND.get(), MaterialInit.DIAMOND.get(),
                MaterialInit.IRON.get(), ToolMaterials.DURUSIUM.get(), MaterialInit.IRON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        ((SynthesisedToolItem) wand.getItem()).reloadComponent(player, helper.getLevel(), wand);
        wand.setDamageValue(damage);
        wand.set(DataComponents.CUSTOM_NAME, Component.literal("Mender fixture"));
        return wand;
    }
}
