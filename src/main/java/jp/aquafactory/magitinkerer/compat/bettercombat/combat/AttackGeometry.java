package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.stln.magitech.helper.CombatHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class AttackGeometry {
    private static final double EPSILON = 1.0E-7;
    private static final List<Vec3> WORLD_AXES = List.of(
            new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));

    private AttackGeometry() {}

    public record Box(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                      double halfWidth, double halfHeight, double halfDepth) {
        public AABB bounds() {
            double x = Math.abs(right.x) * halfWidth + Math.abs(up.x) * halfHeight
                    + Math.abs(forward.x) * halfDepth;
            double y = Math.abs(right.y) * halfWidth + Math.abs(up.y) * halfHeight
                    + Math.abs(forward.y) * halfDepth;
            double z = Math.abs(right.z) * halfWidth + Math.abs(up.z) * halfHeight
                    + Math.abs(forward.z) * halfDepth;
            return new AABB(center.x - x, center.y - y, center.z - z,
                    center.x + x, center.y + y, center.z + z);
        }

        public boolean intersects(AABB target) {
            return firstContact(target, Vec3.ZERO).isPresent();
        }

        /**
         * SAT の各軸で重なる時間区間を求める。奥行きゼロの面を動かすと、
         * 幅・高さのある捕捉面が最初に身体へ触れる距離も同じ計算で得られる。
         */
        public OptionalDouble firstContact(AABB target, Vec3 movement) {
            List<Vec3> localAxes = List.of(right, up, forward);
            List<Vec3> axes = new ArrayList<>(WORLD_AXES);
            axes.addAll(localAxes);
            for (Vec3 world : WORLD_AXES) {
                for (Vec3 local : localAxes) axes.add(world.cross(local));
            }
            Vec3 delta = target.getCenter().subtract(center);
            double enter = 0;
            double exit = 1;
            for (Vec3 axis : axes) {
                double lengthSquared = axis.lengthSqr();
                if (lengthSquared < EPSILON * EPSILON) continue;
                double radius = Math.abs(axis.dot(right)) * halfWidth
                        + Math.abs(axis.dot(up)) * halfHeight
                        + Math.abs(axis.dot(forward)) * halfDepth
                        + (Math.abs(axis.x) * target.getXsize() + Math.abs(axis.y) * target.getYsize()
                        + Math.abs(axis.z) * target.getZsize()) * 0.5
                        + EPSILON * Math.sqrt(lengthSquared);
                double offset = delta.dot(axis);
                double speed = movement.dot(axis);
                if (Math.abs(speed) < EPSILON * Math.sqrt(lengthSquared)) {
                    if (Math.abs(offset) > radius) return OptionalDouble.empty();
                } else {
                    double first = (offset - radius) / speed;
                    double last = (offset + radius) / speed;
                    double axisEnter = Math.min(first, last);
                    double axisExit = Math.max(first, last);
                    enter = Math.max(enter, axisEnter);
                    exit = Math.min(exit, axisExit);
                    if (enter > exit) return OptionalDouble.empty();
                }
            }
            return OptionalDouble.of(enter);
        }
    }

    public static Optional<Box> resolve(Level level, Player player, double reach, double sweep) {
        if (!Double.isFinite(reach) || reach < 0 || !Double.isFinite(sweep) || sweep <= 0)
            return Optional.empty();
        Vec3 eye = player.getEyePosition();
        Vec3 forward = Vec3.directionFromRotation(player.getRotationVector()).normalize();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        // yaw から右軸を作るため、真上・真下を向いても基底が退化しない。
        Vec3 up = forward.cross(right).normalize();
        double minimum = Math.max(reach, sweep) * 0.5;

        // S が R を大きく上回る場合も、最低距離へ押し出す途中の壁を飛び越さない。
        Vec3 blockEnd = eye.add(forward.scale(Math.max(reach, minimum)));
        var block = level.clip(new ClipContext(eye, blockEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDistance = block.getType() == HitResult.Type.BLOCK
                ? block.getLocation().distanceTo(eye) : Double.POSITIVE_INFINITY;
        Box capture = new Box(eye, right, up, forward, sweep * 0.5, sweep * 0.25, 0);
        Vec3 movement = forward.scale(reach);
        AABB captureBounds = capture.bounds().minmax(capture.bounds().move(movement)).inflate(EPSILON);
        double nearest = Double.POSITIVE_INFINITY;
        for (Entity candidate : level.getEntities(player, captureBounds,
                entity -> eligible(player, entity))) {
            if (!visible(level, player, candidate)) continue;
            OptionalDouble contact = capture.firstContact(candidate.getBoundingBox(), movement);
            if (contact.isPresent()) nearest = Math.min(nearest, contact.getAsDouble() * reach);
        }

        double depth = sweep;
        double distance;
        if (blockDistance < minimum) {
            // 手前の敵よりも壁を優先し、箱を壁の向こうへ丸ごと押し出さない。
            distance = blockDistance;
            depth *= blockDistance / minimum;
        } else {
            double detected = Math.min(nearest, blockDistance);
            distance = Double.isFinite(detected) ? Math.max(minimum, detected) : minimum;
        }
        return Optional.of(new Box(eye.add(forward.scale(distance)), right, up, forward,
                sweep * 0.5, sweep * 0.25, depth * 0.5));
    }

    public static List<Entity> findTargets(Level level, Player player, double reach, double sweep) {
        Optional<Box> resolved = resolve(level, player, reach, sweep);
        if (resolved.isEmpty() || resolved.get().halfDepth() <= 0) return List.of();
        Box box = resolved.get();
        return findTargets(level, player, box);
    }

    public static List<Entity> findTargets(Level level, Player player, Box box) {
        if (box.halfDepth() <= 0) return List.of();
        // 外接 AABB は候補収集だけに使い、斜めの余白へ当たり判定を広げない。
        return level.getEntities(player, box.bounds().inflate(EPSILON),
                entity -> eligible(player, entity) && box.intersects(entity.getBoundingBox())
                        && visible(level, player, entity));
    }

    private static boolean eligible(Player player, Entity entity) {
        return entity instanceof LivingEntity living && entity != player && !entity.isSpectator()
                && entity.isAttackable() && !entity.isInvulnerable() && player.canAttack(living);
    }

    private static boolean visible(Level level, Player player, Entity entity) {
        // 最終攻撃と同じ身体間の遮蔽を捕捉にも適用し、壁越しの敵へ吸わせない。
        return level.clip(new ClipContext(CombatHelper.getBodyPos(entity), CombatHelper.getBodyPos(player),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.BLOCK;
    }
}
