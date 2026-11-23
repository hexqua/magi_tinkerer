package jp.aquafactory.magitinkerer.gametest;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;
import net.stln.magitech.feature.tool.trait.CollapseTrait;
import net.stln.magitech.feature.tool.trait.PrecipitationTrait;
import net.stln.magitech.helper.BlockHelper;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class SpellweavingMiningGameTests {
    @GameTest(template = "gametest/combat_geometry")
    public static void connectedMiningAndTraitActivationFollowSpellweavingGroups(GameTestHelper helper) {
        var level = helper.getLevel();
        boolean hasMalum = ModList.get().isLoaded(MagiTinkerer.MALUM_MOD_ID);
        Block origin = hasMalum ? malumBlock("runewood_log") : Blocks.OAK_LOG;
        Block variant = hasMalum ? malumBlock("exposed_runewood_log") : Blocks.STRIPPED_OAK_LOG;
        Block unrelated = hasMalum ? malumBlock("soulwood_log") : Blocks.SPRUCE_LOG;

        var row = new BlockPos(3, 2, 3);
        helper.setBlock(row, origin);
        helper.setBlock(row.west(), origin);
        helper.setBlock(row.east(), variant);
        helper.setBlock(row.east(2), unrelated);
        var column = new BlockPos(10, 2, 10);
        helper.setBlock(column, origin);
        helper.setBlock(column.above(), variant);
        helper.setBlock(column.above(2), origin);
        helper.setBlock(column.above(3), variant);

        var rowCenter = helper.absolutePos(row);
        var same = helper.absolutePos(row.west());
        var related = helper.absolutePos(row.east());
        var expectedRow = hasMalum ? Set.of(rowCenter, same, related) : Set.of(rowCenter, same);
        var connected = BlockHelper.getConnectedBlocks(level, rowCenter, origin, 15);
        helper.assertTrue(connected.equals(expectedRow),
                "Connected mining should include only the matching logs: " + connected);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var iron = MaterialInit.IRON.get();
        var stack = SynthesisedToolGenerator.generateTool(ToolTypeInit.AXE.get(), iron);
        var properties = ((SynthesisedToolItem) stack.getItem()).getAppliedProperties(player, level, stack);
        var collapse = new HashSet<BlockPos>();
        new CollapseTrait().additionalBlockBreak(player, level, stack, 1, properties,
                level.getBlockState(rowCenter), rowCenter, collapse, 1, Direction.UP, false);
        helper.assertTrue(collapse.equals(hasMalum ? expectedRow : Set.of(rowCenter)),
                "Collapse should count a Spellweaving variant towards the required width: " + collapse);

        var columnBase = helper.absolutePos(column);
        var precipitation = new HashSet<BlockPos>();
        new PrecipitationTrait().additionalBlockBreak(player, level, stack, 1, properties,
                level.getBlockState(columnBase), columnBase, precipitation, 1, Direction.UP, false);
        var expectedColumn = hasMalum
                ? Set.of(columnBase, columnBase.above(), columnBase.above(2), columnBase.above(3))
                : Set.of(columnBase);
        helper.assertTrue(precipitation.equals(expectedColumn),
                "Precipitation should count Spellweaving variants towards the required height: " + precipitation);
        helper.succeed();
    }

    private static Block malumBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MALUM_MOD_ID, id))
                .orElseThrow(() -> new IllegalStateException("Missing Malum block: " + id));
    }
}
