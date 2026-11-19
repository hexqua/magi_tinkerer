package jp.aquafactory.magitinkerer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(MagiTinkerer.MODID)
public class MagiTinkerer {
    public static final String MODID = "magitinkerer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagiTinkerer(IEventBus modEventBus, ModContainer modContainer) {
    }
}
