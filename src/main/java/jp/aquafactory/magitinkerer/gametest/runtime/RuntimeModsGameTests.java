package jp.aquafactory.magitinkerer.gametest.runtime;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class RuntimeModsGameTests {
    @GameTest(template = "gametest/runtime")
    public static void dependencyProfileMatchesRun(GameTestHelper helper) {
        var mods = ModList.get();
        helper.assertTrue(mods.isLoaded("magitech"), "Magitech is missing");
        helper.assertTrue(mods.isLoaded("atlas_api"), "Atlas API is missing");
        boolean expectBetterCombat = Boolean.getBoolean("magitinkerer.expectBetterCombat");
        helper.assertTrue(mods.isLoaded(MagiTinkerer.BETTER_COMBAT_MOD_ID) == expectBetterCombat,
                "Expected Better Combat loaded: " + expectBetterCombat);
        helper.assertTrue(mods.isLoaded("cloth_config") == expectBetterCombat,
                "Expected Cloth Config API loaded: " + expectBetterCombat);
        helper.succeed();
    }
}
