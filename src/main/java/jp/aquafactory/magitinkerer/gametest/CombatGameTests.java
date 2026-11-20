package jp.aquafactory.magitinkerer.gametest;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.gametest.bettercombat.AttackGeometryGameTests;
import jp.aquafactory.magitinkerer.gametest.bettercombat.SlashVisualGameTests;
import jp.aquafactory.magitinkerer.gametest.bettercombat.WeaponCombatGameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = MagiTinkerer.MOD_ID)
public final class CombatGameTests {
    private CombatGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded(MagiTinkerer.BETTER_COMBAT_MOD_ID)) {
            event.register(AttackGeometryGameTests.class);
            event.register(SlashVisualGameTests.class);
            event.register(WeaponCombatGameTests.class);
        } else {
            event.register(MagitechCombatGameTests.class);
        }
    }
}
