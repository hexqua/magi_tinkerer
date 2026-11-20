package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.neoforged.bus.api.IEventBus;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.material.ToolMaterial;
import net.stln.magitech.feature.tool.trait.Trait;
import net.stln.magitech.registry.DeferredToolMaterial;
import net.stln.magitech.registry.DeferredToolMaterialRegister;
import net.stln.magitech.registry.DeferredTrait;
import net.stln.magitech.registry.DeferredTraitRegister;

public final class ToolMaterials {
    private static final DeferredTraitRegister TRAITS = new DeferredTraitRegister(MagiTinkerer.MOD_ID);
    private static final DeferredToolMaterialRegister MATERIALS = new DeferredToolMaterialRegister(MagiTinkerer.MOD_ID);

    private static final DeferredTrait<Trait> SEA_FRIENDS = TRAITS.register("sea_friends", SeaFriendsTrait::new);

    public static final DeferredToolMaterial<ToolMaterial> PRISMARINE_CRYSTAL =
            MATERIALS.register("prismarine_crystal", () -> new ToolMaterial(
                            MaterialInit.SAMPLE.get().order(), // todo:それっぽい値にする.
                            () -> MaterialInit.SAMPLE.get().properties().get().copy(), // todo:それっぽい値にする.
                            SEA_FRIENDS
                    )
            );

    private ToolMaterials() {
    }

    public static void register(IEventBus modBus) {
        TRAITS.register(modBus);
        MATERIALS.register(modBus);
    }
}
