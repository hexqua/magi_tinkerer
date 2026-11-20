package jp.aquafactory.magitinkerer.gametest.bettercombat;

import com.google.gson.Gson;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackGeometry;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SlashVisual;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class SlashVisualGameTests {
    private SlashVisualGameTests() {}

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void explicitTrailOverridesAnimationAndIgnoresHitboxAngle(GameTestHelper helper) {
        String animation = "magitinkerer:test_slash";
        var map = BetterCombatMod.trailConfig.value.animation_based;
        var previous = map.put(animation, List.of(new ParticlePlacement("slash180", 0, 0, 0, 0, 0, -30)));
        try {
            var explicit = attack("""
                    {"animation":"magitinkerer:test_slash","angle":0,
                     "trail_particles":[{"particle_type":"slash90","roll_set":12}]}
                    """);
            var changedAngle = attack("""
                    {"animation":"magitinkerer:test_slash","angle":170,
                     "trail_particles":[{"particle_type":"slash90","roll_set":12}]}
                    """);
            float roll = SlashVisual.resolveRolls(explicit, false, false, RandomSource.create(1)).getFirst();
            helper.assertTrue(Math.abs(roll - 12) <= 5, "Explicit placement was not used");
            helper.assertTrue(roll == SlashVisual.resolveRolls(changedAngle, false, false,
                    RandomSource.create(1)).getFirst(), "Hitbox angle changed the visual");
            var inherited = attack("{\"animation\":\"magitinkerer:test_slash\"}");
            float inheritedRoll = SlashVisual.resolveRolls(inherited, false, false, RandomSource.create(1)).getFirst();
            helper.assertTrue(Math.abs(inheritedRoll + 30) <= 5, "Animation trail fallback was not used");
        } finally {
            if (previous == null) map.remove(animation);
            else map.put(animation, previous);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void handednessMirrorsFlowAndJitterStaysSmall(GameTestHelper helper) {
        var attack = attack("""
                {"trail_particles":[{"particle_type":"slash180","roll_set":0}]}
                """);
        var box = new AttackGeometry.Box(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                new Vec3(0, 0, 1), 2, 1, 2);
        for (int seed = 0; seed < 32; seed++) {
            float main = SlashVisual.resolveRolls(attack, false, false, RandomSource.create(seed)).getFirst();
            float off = SlashVisual.resolveRolls(attack, true, false, RandomSource.create(seed)).getFirst();
            float left = SlashVisual.resolveRolls(attack, false, true, RandomSource.create(seed)).getFirst();
            float leftOff = SlashVisual.resolveRolls(attack, true, true, RandomSource.create(seed)).getFirst();
            helper.assertTrue(Math.abs(main) <= 5 && Math.abs(off + 180) <= 5,
                    "Random tilt exceeded five degrees");
            helper.assertTrue(off == left && main == leftOff, "Offhand and left-handed mirroring differ");
            var visual = SlashVisual.from(box, List.of(main, off), 0, 0);
            helper.assertTrue(visual.point(main, 0).x < 0 && visual.point(main, 1).x > 0
                            && visual.point(off, 0).x > 0 && visual.point(off, 1).x < 0,
                    "Mirrored slash did not reverse flow");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void slashPointsFollowPitchedAndWallShortenedBox(GameTestHelper helper) {
        for (float pitch : new float[]{0, 45, -90, 90}) {
            Vec3 forward = Vec3.directionFromRotation(pitch, 0).normalize();
            Vec3 right = new Vec3(1, 0, 0);
            Vec3 up = forward.cross(right).normalize();
            var box = new AttackGeometry.Box(new Vec3(10, 20, 30), right, up, forward, 4, 2, 0.25);
            var visual = SlashVisual.from(box, List.of(-5F, 185F), 0, 0);
            for (float roll : visual.rolls()) {
                for (int index = 0; index <= 64; index++) {
                    var point = visual.point(roll, index / 64.0).subtract(box.center());
                    helper.assertTrue(Math.abs(point.dot(right)) <= 4.00001
                                    && Math.abs(point.dot(up)) <= 2.00001
                                    && point.dot(forward) >= -0.00001 && point.dot(forward) <= 0.25001,
                            "Visual exceeded the pitched or shortened hitbox");
                }
            }
        }
        var unknown = attack("{\"animation\":\"unknown:motion\"}");
        helper.assertTrue(Math.abs(SlashVisual.resolveRolls(unknown, false, false,
                RandomSource.create(1)).getFirst()) <= 5, "Unknown animation did not fall back to horizontal");
        helper.succeed();
    }

    private static WeaponAttributes.Attack attack(String json) {
        return new Gson().fromJson(json, WeaponAttributes.Attack.class);
    }
}
