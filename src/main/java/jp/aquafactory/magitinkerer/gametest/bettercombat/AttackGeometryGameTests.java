package jp.aquafactory.magitinkerer.gametest.bettercombat;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackGeometry;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.CombatServer;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackRequest;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.entity.AdjustableAttackStrengthEntity;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;

import java.util.Objects;
import java.util.UUID;

@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class AttackGeometryGameTests {
    private static final String TEMPLATE = "gametest/combat_geometry";
    private static final Vec3 ORIGIN = new Vec3(8, 3, 3);
    private static final double EPSILON = 1.0E-5;

    private AttackGeometryGameTests() {}

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void obbRejectsBroadBoundsCornersAndTracksPitch(GameTestHelper helper) {
        for (float yaw : new float[]{0, 45, 120}) {
            for (float pitch : new float[]{0, 35, 90, -90}) {
                Vec3 forward = Vec3.directionFromRotation(pitch, yaw).normalize();
                Vec3 right = new Vec3(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw)));
                Vec3 up = forward.cross(right).normalize();
                var box = new AttackGeometry.Box(Vec3.ZERO, right, up, forward, 2, 1, 2);
                Vec3 inside = right.scale(1.8).add(up.scale(0.8)).add(forward.scale(1.8));
                helper.assertTrue(box.intersects(pointBox(inside)), "OBB lost an interior target");
                helper.assertTrue(!box.intersects(pointBox(right.scale(2.2))), "OBB exceeded its local width");
                helper.assertTrue(!box.intersects(pointBox(up.scale(1.2))), "OBB exceeded its local height");
                helper.assertTrue(!box.intersects(pointBox(forward.scale(2.2))), "OBB exceeded its local depth");
                if (yaw == 45 && pitch == 0) {
                    AABB corner = pointBox(right.scale(2.5));
                    helper.assertTrue(box.bounds().intersects(corner), "Fixture is not in the broad bounds corner");
                    helper.assertTrue(!box.intersects(corner), "Broad-phase AABB became the final hitbox");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void emptySwingUsesLargerStatAndNeverExtendsBehindLook(GameTestHelper helper) {
        FakePlayer player = player(helper);
        for (double[] stats : new double[][]{{6, 4}, {3, 8}, {4, 4}}) {
            var box = AttackGeometry.resolve(helper.getLevel(), player, stats[0], stats[1]).orElseThrow();
            close(helper, box.center().subtract(player.getEyePosition()).dot(box.forward()),
                    Math.max(stats[0], stats[1]) / 2, "Empty swing center");
            close(helper, box.halfWidth() * 2, stats[1], "Box width");
            close(helper, box.halfHeight() * 2, stats[1] / 2, "Box height");
            close(helper, box.halfDepth() * 2, stats[1], "Box depth");
            helper.assertTrue(!box.intersects(pointBox(player.getEyePosition().subtract(box.forward().scale(0.1)))),
                    "Empty swing extends behind the player");
        }
        player.setXRot(-90);
        var upward = AttackGeometry.resolve(helper.getLevel(), player, 6, 4).orElseThrow();
        close(helper, upward.center().y - player.getEyePosition().y, 3, "Upward center");
        helper.assertTrue(!upward.intersects(pointBox(player.getEyePosition().subtract(0, 0.1, 0))),
                "Upward box extends behind the look direction");
        helper.assertTrue(AttackGeometry.resolve(helper.getLevel(), player, Double.NaN, 4).isEmpty(),
                "Non-finite reach was accepted");
        helper.assertTrue(AttackGeometry.resolve(helper.getLevel(), player, 6, 0).isEmpty(),
                "Zero area was accepted");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void wideCaptureFindsOffAxisEnemyThroughActualAttackRoute(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = SynthesisedToolGenerator.generateTool(ToolTypeInit.SCYTHE.get(), MaterialInit.SAMPLE);
        ((SynthesisedToolItem) weapon.getItem()).reloadComponent(player, player.level(), weapon);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        // Magitech が inventory tick で component を更新してから装備属性を適用する。
        player.doTick();
        for (var entry : weapon.getAttributeModifiers().modifiers()) {
            var attribute = player.getAttribute(entry.attribute());
            if (attribute != null) attribute.addOrUpdateTransientModifier(entry.modifier());
        }
        player.setYRot(0);
        player.setXRot(0);
        resetCombo(player);
        Cow target = target(helper, 1.6, 0, 3.5);
        var stats = ((SynthesisedToolItem) weapon.getItem()).getAppliedProperties(player, player.level(), weapon);
        var expectedBox = AttackGeometry.resolve(player.level(), player, player.entityInteractionRange(),
                stats.getOrId(ToolPropertyInit.SWEEP.get()).floatValue()).orElseThrow();
        ((AdjustableAttackStrengthEntity) player).setLastAttackedTicks(100);
        var result = CombatServer.attack(player, new AttackRequest(1, 0, 0));
        helper.assertTrue(result.isPresent() && target.getHealth() < 200,
                "Wide off-axis attack failed: accepted=" + result.isPresent()
                        + ", health=" + target.getHealth() + ", reach=" + player.entityInteractionRange()
                        + ", modifiers=" + weapon.getAttributeModifiers());
        helper.assertTrue(result.orElseThrow().targets().length == 1 && weapon.getDamageValue() == 1,
                "Wide capture duplicated hit or durability effects");
        var visual = result.orElseThrow().visual().orElseThrow();
        helper.assertTrue(visual.center().distanceTo(expectedBox.center()) < EPSILON
                        && visual.forward().distanceTo(expectedBox.forward()) < EPSILON
                        && Math.abs(visual.halfDepth() - expectedBox.halfDepth()) < EPSILON,
                "Visual did not snapshot the server hitbox: expected=" + expectedBox + ", actual=" + visual);
        Vec3 point = visual.point(visual.rolls().getFirst(), 0.5);
        player.setPos(player.position().add(10, 5, 0));
        player.setYRot(180);
        helper.assertTrue(visual.point(visual.rolls().getFirst(), 0.5).equals(point),
                "Visual followed player movement after the hit");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void nearerEnemyWinsAndMinimumCenterStillApplies(GameTestHelper helper) {
        FakePlayer player = player(helper);
        Cow near = target(helper, 1.5, 0, 1.5);
        Cow far = target(helper, 0, 0, 5.5);
        var box = AttackGeometry.resolve(helper.getLevel(), player, 6, 4).orElseThrow();
        close(helper, box.center().z - player.getEyePosition().z, 3, "Near capture minimum center");
        var hits = AttackGeometry.findTargets(helper.getLevel(), player, 6, 4);
        helper.assertTrue(hits.contains(near) && !hits.contains(far), "Far enemy stole the attack center");
        helper.assertTrue(!box.intersects(pointBox(player.getEyePosition().subtract(0, 0, 0.1))),
                "Near enemy moved the box behind the player");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void nonLivingAndOccludedEntitiesCannotStealCapture(GameTestHelper helper) {
        FakePlayer player = player(helper);
        helper.spawn(EntityType.MINECART, ORIGIN.add(0, 1, 0.8));
        Cow hidden = target(helper, 1.5, 0, 2);
        helper.setBlock(new BlockPos(9, 4, 4), Blocks.STONE);
        Cow visible = target(helper, 0, 0, 5.5);
        var box = AttackGeometry.resolve(helper.getLevel(), player, 6, 4).orElseThrow();
        helper.assertTrue(box.center().z - player.getEyePosition().z > 4.5,
                "Non-living or hidden entity stole the capture");
        var hits = AttackGeometry.findTargets(helper.getLevel(), player, 6, 4);
        helper.assertTrue(hits.contains(visible) && !hits.contains(hidden),
                "Visibility rules did not match capture and final damage");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void closeBlockOverridesNearEnemyAndOnlyShortensDepth(GameTestHelper helper) {
        FakePlayer player = player(helper);
        Cow near = target(helper, -1.5, 0, 1);
        Cow behindWall = target(helper, 0, 0, 3);
        helper.setBlock(new BlockPos(8, 4, 5), Blocks.STONE);
        var box = AttackGeometry.resolve(helper.getLevel(), player, 6, 4).orElseThrow();
        close(helper, box.center().z - player.getEyePosition().z, 2, "Close block center");
        close(helper, box.halfDepth() * 2, 8.0 / 3, "Close block depth");
        close(helper, box.halfWidth(), 2, "Close block preserves width");
        close(helper, box.halfHeight(), 1, "Close block preserves height");
        helper.assertTrue(!box.intersects(pointBox(player.getEyePosition().subtract(0, 0, 0.1))),
                "Close block box extends backwards");
        var hits = AttackGeometry.findTargets(helper.getLevel(), player, 6, 4);
        helper.assertTrue(hits.contains(near) && !hits.contains(behindWall),
                "Block override lost the visible target or pierced the wall");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void largeAreaChecksBlocksBeyondOriginalReach(GameTestHelper helper) {
        FakePlayer player = player(helper);
        helper.setBlock(new BlockPos(8, 4, 6), Blocks.STONE);
        var box = AttackGeometry.resolve(helper.getLevel(), player, 2, 8).orElseThrow();
        close(helper, box.center().z - player.getEyePosition().z, 3, "Block between reach and minimum");
        close(helper, box.halfDepth() * 2, 6, "Large area block depth");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void areaCanHitPastReachButCannotCapturePastReach(GameTestHelper helper) {
        FakePlayer player = player(helper);
        Cow beyond = target(helper, 0, 0, 6.9);
        var empty = AttackGeometry.resolve(helper.getLevel(), player, 6, 4).orElseThrow();
        close(helper, empty.center().z - player.getEyePosition().z, 3, "Capture must stop at reach");
        helper.assertTrue(!AttackGeometry.findTargets(helper.getLevel(), player, 6, 4).contains(beyond),
                "Out-of-reach enemy stole the center");
        Cow edge = target(helper, 0, 0, 5.8);
        var hits = AttackGeometry.findTargets(helper.getLevel(), player, 6, 4);
        helper.assertTrue(hits.contains(edge) && hits.contains(beyond), "Area did not extend beyond capture reach");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = TEMPLATE)
    public static void pitchTracksCaptureAndDamageInBothDirections(GameTestHelper helper) {
        FakePlayer player = player(helper);
        player.setXRot(-45);
        Cow above = target(helper, 0, 3, 3);
        Cow horizontal = target(helper, 0, 0, 3);
        var upwardHits = AttackGeometry.findTargets(helper.getLevel(), player, 6, 2);
        helper.assertTrue(upwardHits.contains(above) && !upwardHits.contains(horizontal),
                "Upward capture and OBB do not share pitch");
        above.discard();
        horizontal.discard();
        player.setPos(helper.absoluteVec(ORIGIN.add(0, 5, 0)));
        player.setXRot(45);
        Cow below = target(helper, 0, 3, 3);
        helper.assertTrue(AttackGeometry.findTargets(helper.getLevel(), player, 6, 2).contains(below),
                "Downward attack did not follow pitch");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "geometry-test"));
        player.setPos(helper.absoluteVec(ORIGIN));
        player.setYRot(0);
        player.setXRot(0);
        resetCombo(player);
        return player;
    }

    private static void resetCombo(Player player) {
        ((PlayerAttackProperties) player).setComboCount(-1);
    }

    private static Cow target(GameTestHelper helper, double x, double y, double z) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, ORIGIN.add(x, y, z));
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(200);
        cow.setHealth(200);
        return cow;
    }

    private static AABB pointBox(Vec3 center) {
        return new AABB(center.x - 0.01, center.y - 0.01, center.z - 0.01,
                center.x + 0.01, center.y + 0.01, center.z + 0.01);
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < EPSILON,
                message + ": expected " + expected + ", got " + actual);
    }
}
