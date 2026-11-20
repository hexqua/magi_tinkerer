package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.resources.ResourceLocation;
import net.stln.magitech.feature.tool.trait.Trait;

import java.awt.Color;

final class SoulhunterTrait extends Trait {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "soulhunter");

    @Override
    public Color getColor() {
        return new Color(0x985DAB);
    }

    @Override
    public ResourceLocation getKey() {
        return ID;
    }
}
