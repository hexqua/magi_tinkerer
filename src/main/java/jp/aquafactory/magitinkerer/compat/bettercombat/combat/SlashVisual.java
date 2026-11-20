package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record SlashVisual(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                          double halfWidth, double halfHeight, double halfDepth,
                          List<Float> rolls, int primary, int secondary) {
    public SlashVisual {
        rolls = List.copyOf(rolls);
    }

    public static List<Float> resolveRolls(WeaponAttributes.Attack attack, boolean offHand,
                                           boolean leftHanded, RandomSource random) {
        List<ParticlePlacement> placements = attack.trailParticles();
        if (placements.isEmpty()) {
            var configured = BetterCombatMod.trailConfig.value.animation_based.get(attack.animation());
            if (configured != null) placements = configured;
        }
        // 未知の表現や突きに対して判定形状は変更せず、水平の弧へフォールバックする。
        var baseRolls = placements.stream()
                .filter(placement -> placement.particle_type() != null
                        && placement.particle_type().matches("slash(45|90|180|270|360)"))
                .map(ParticlePlacement::roll_set).filter(Float::isFinite).toList();
        if (baseRolls.isEmpty()) baseRolls = List.of(0F);
        boolean mirror = offHand ^ leftHanded;
        return baseRolls.stream().map(roll -> {
            float mirrored = mirror ? -(roll + 180F) : roll;
            return mirrored + (random.nextFloat() * 2 - 1) * 5F;
        }).toList();
    }

    public static SlashVisual from(AttackGeometry.Box box, List<Float> rolls, int primary, int secondary) {
        return new SlashVisual(box.center(), box.right(), box.up(), box.forward(),
                box.halfWidth(), box.halfHeight(), box.halfDepth(), rolls, primary, secondary);
    }

    public Vec3 point(float roll, double progress) {
        double angle = (progress - 0.5) * Math.PI;
        double radians = Math.toRadians(roll);
        // 前半分の楕円弧を判定箱内へ置く。壁で奥行きが縮んでも中心を再捕捉しない。
        double lateral = Math.sin(angle);
        return center.add(right.scale(lateral * halfWidth * Math.cos(radians)))
                .add(up.scale(lateral * Math.min(halfWidth, halfHeight) * Math.sin(radians)))
                .add(forward.scale(Math.cos(angle) * halfDepth));
    }
}
