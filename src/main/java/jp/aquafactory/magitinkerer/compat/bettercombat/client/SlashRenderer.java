package jp.aquafactory.magitinkerer.compat.bettercombat.client;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SlashVisual;
import net.minecraft.world.level.Level;
import net.stln.magitech.effect.visual.TrailRenderHelper;
import net.stln.magitech.effect.visual.preset.PresetHelper;
import net.stln.magitech.effect.visual.spawner.SquareParticles;
import net.stln.magitech.effect.visual.trail.TrailData;
import net.stln.magitech.effect.visual.trail.TrailRenderer;
import team.lodestar.lodestone.systems.rendering.trail.InterpolatedTrailPoint;
import team.lodestar.lodestone.systems.rendering.trail.TrailPointBuilder;

import java.awt.Color;

public final class SlashRenderer {
    private static final int SEGMENTS = 64;
    private static final int LIFETIME = 20;

    private SlashRenderer() {}

    public static void show(Level level, SlashVisual visual) {
        if (visual.halfWidth() <= 0 || visual.halfDepth() <= 0) return;
        Color primary = new Color(visual.primary(), true);
        Color secondary = new Color(visual.secondary(), true);
        for (float roll : visual.rolls()) {
            TrailPointBuilder trail = TrailPointBuilder.create(LIFETIME);
            for (int index = 0; index <= SEGMENTS; index++) {
                double progress = (double) index / SEGMENTS;
                var point = visual.point(roll, progress);
                trail.addTrailPoint(new InterpolatedTrailPoint(point, point, LIFETIME));
                // 始点側を先に消し、モーションの振り抜く方向へ残光を流す。
                if (index % 4 == 3) trail.tickTrailPoints();
                if (index % 4 == 0 && index < SEGMENTS) {
                    var tangent = visual.point(roll, progress + 1.0 / SEGMENTS).subtract(point).normalize();
                    var particles = SquareParticles.squareParticleColored(level, point, primary, secondary);
                    PresetHelper.modify(particles, builder -> builder.setMotion(tangent.scale(0.07)));
                    particles.spawnParticles();
                }
            }
            TrailRenderer.add(new TrailData(level, TrailRenderHelper.defaultBuilderFunc(), trail,
                    primary, secondary, 0.25F, 0.9F));
            TrailRenderer.add(new TrailData(level, TrailRenderHelper.defaultBuilderFunc(), trail,
                    primary, secondary, 0.5F, 0.5F));
        }
    }
}
