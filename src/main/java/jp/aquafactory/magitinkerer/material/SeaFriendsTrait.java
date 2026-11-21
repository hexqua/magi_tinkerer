package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.stln.magitech.feature.tool.property.ToolProperties;
import net.stln.magitech.feature.tool.trait.Trait;

import java.awt.Color;

final class SeaFriendsTrait extends Trait {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "sea_friends");

    @Override
    public float modifyMiningSpeed(Player player, Level level, ItemStack stack, int traitLevel,
                                   ToolProperties properties, BlockState state, BlockPos pos) {
        //noinspection deprecation
        if (!player.isEyeInFluid(FluidTags.WATER)) {
            return 1.0F;
        }

        float submergedSpeed = (float) player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        float multiplier = player.onGround() ? 1.0F : 5.0F;
        if (submergedSpeed > 0.0F && submergedSpeed < 1.0F) {
            multiplier /= submergedSpeed;
        }
        return multiplier;
    }

    @Override
    public Color getColor() {
        return new Color(0x91C5B7);
    }

    @Override
    public ResourceLocation getKey() {
        return ID;
    }
}
