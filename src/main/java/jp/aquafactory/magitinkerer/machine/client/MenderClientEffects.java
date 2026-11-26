package jp.aquafactory.magitinkerer.machine.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.stln.magitech.effect.visual.preset.PointVFX;
import net.stln.magitech.effect.visual.preset.PresetHelper;
import net.stln.magitech.effect.visual.spawner.SquareParticles;
import net.stln.magitech.feature.element.Element;
import team.lodestar.lodestone.systems.particle.ParticleEffectSpawner;

public final class MenderClientEffects {
    private MenderClientEffects() {
    }

    public static void spawnRepairParticles(Level level, BlockPos pos) {
        Vec3 origin = Vec3.atBottomCenterOf(pos).add(0, 0.46, 0);
        PointVFX.spray(level, origin, Element.MANA, MenderClientEffects::repairParticle,
                new Vec3(0, 1, 0), 6, 0.035F, 0.055F);
    }

    private static ParticleEffectSpawner repairParticle(Level level, Vec3 position, Element element) {
        int lifetime = level.random.nextInt(7, 11);
        var particle = PresetHelper.smaller(SquareParticles.squareShrinkParticle(level, position, element));
        return PresetHelper.modify(particle, builder -> builder.setLifetime(lifetime));
    }
}
