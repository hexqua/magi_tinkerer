package jp.aquafactory.magitinkerer;

import jp.aquafactory.magitinkerer.compat.bettercombat.client.ClientCombat;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.CombatNetwork;
import jp.aquafactory.magitinkerer.datagen.DataGenerators;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import jp.aquafactory.magitinkerer.material.client.ToolMaterialAssetHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(MagiTinkerer.MOD_ID)
public final class MagiTinkerer {
    public static final String MOD_ID = "magitinkerer";
    public static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    public MagiTinkerer(IEventBus modBus, Dist dist) {
        ToolMaterials.register(modBus);
        modBus.addListener(DataGenerators::gatherData);
        if (dist == Dist.CLIENT) {
            ToolMaterialAssetHandler.register(modBus);
        }

        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            modBus.addListener(CombatNetwork::register);
            if (dist == Dist.CLIENT) {
                modBus.addListener(ClientCombat::setup);
            }
        }
    }
}
