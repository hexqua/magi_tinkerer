package jp.aquafactory.magitinkerer.machine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.magitinkerer.machine.ManaDurumMenderBlockEntity;
import jp.aquafactory.magitinkerer.machine.MenderContent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.NotNull;

public final class ManaDurumMenderRenderer implements BlockEntityRenderer<ManaDurumMenderBlockEntity> {
    private static final int ROTATION_PERIOD_TICKS = 240;
    private final ItemRenderer itemRenderer;

    private ManaDurumMenderRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MenderContent.MENDER_ENTITY.get(), ManaDurumMenderRenderer::new);
    }

    @Override
    public void render(@NotNull ManaDurumMenderBlockEntity entity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffers, int light, int overlay) {
        ItemStack item = entity.getItem(0);
        Level level = entity.getLevel();
        if (item.isEmpty() || level == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.40, 0.5);
        float turns = (level.getGameTime() % ROTATION_PERIOD_TICKS + partialTick) / ROTATION_PERIOD_TICKS;
        poseStack.mulPose(Axis.YP.rotationDegrees(turns * 360.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(item, ItemDisplayContext.GROUND, light, overlay, poseStack, buffers, level, 0);
        poseStack.popPose();
    }
}
