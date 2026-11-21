package jp.aquafactory.magitinkerer.material.client;

import jp.aquafactory.magitinkerer.material.MaterialContent;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.IdentityHashMap;
import java.util.function.Function;

public final class ConnectedGlassClientRegistration {
    private ConnectedGlassClientRegistration() {
    }

    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        wrap(event, MaterialContent.MANEUVERING_COATED_GLASS.get(), MaterialContent.MANEUVERING_COATED_GLASS_PANE.get(),
                MaterialContent.MANEUVERING_COATED_GLASS.getId());
        wrap(event, MaterialContent.RESTRAINT_COATED_GLASS.get(), MaterialContent.RESTRAINT_COATED_GLASS_PANE.get(),
                MaterialContent.RESTRAINT_COATED_GLASS.getId());
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block glass, Block pane, ResourceLocation id) {
        var texture = id.withPrefix("block/");
        var textures = event.getTextureGetter();
        var original = textures.apply(new Material(InventoryMenu.BLOCK_ATLAS, texture));
        var connected = textures.apply(new Material(InventoryMenu.BLOCK_ATLAS, texture.withSuffix("_connected")));
        var missing = textures.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
        if (original == missing || connected == missing) {
            return;
        }

        wrapStates(event, glass, model -> new ConnectedGlassModel(model, original, connected, false));
        wrapStates(event, pane, model -> new ConnectedGlassModel(model, original, connected, true));
    }

    private static void wrapStates(ModelEvent.ModifyBakingResult event, Block block,
                                   Function<BakedModel, ConnectedGlassModel> wrap) {
        // 同じ multipart モデルを共有する各 state に対し、変換済み quad のキャッシュも共有する。
        var wrappers = new IdentityHashMap<BakedModel, ConnectedGlassModel>();
        for (var state : block.getStateDefinition().getPossibleStates()) {
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                    (location, model) -> wrappers.computeIfAbsent(model, wrap));
        }
    }
}
