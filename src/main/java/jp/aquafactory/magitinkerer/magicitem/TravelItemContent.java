package jp.aquafactory.magitinkerer.magicitem;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class TravelItemContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagiTinkerer.MOD_ID);
    public static final DeferredItem<TravelItem> SIGNUM_THREAD = ITEMS.registerItem("signum_thread",
            properties -> new TravelItem(properties, TravelItem.Kind.THREAD));
    public static final DeferredItem<TravelItem> SPATIAL_MIRROR = ITEMS.registerItem("spatial_distortion_mirror",
            properties -> new TravelItem(properties, TravelItem.Kind.SPATIAL));
    public static final DeferredItem<TravelItem> DIMENSIONAL_MIRROR = ITEMS.registerItem("dimension_distortion_mirror",
            properties -> new TravelItem(properties, TravelItem.Kind.DIMENSIONAL));

    private static final ModConfigSpec.ConfigValue<List<? extends String>> THREAD_DIMENSIONS;
    private static final ModConfigSpec CONFIG;

    static {
        var builder = new ModConfigSpec.Builder();
        THREAD_DIMENSIONS = builder.comment("Dimensions in which the Light-Thread of Signum can be used.")
                .defineListAllowEmpty("signumThreadDimensions", List.of("minecraft:overworld", "minecraft:the_end"),
                        () -> "minecraft:overworld", value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        CONFIG = builder.build();
    }

    private TravelItemContent() {}

    public static void register(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, CONFIG);
        bus.addListener(TravelEffects::register);
    }

    static boolean allowsThread(ResourceLocation dimension) {
        return THREAD_DIMENSIONS.get().stream().map(ResourceLocation::parse).anyMatch(dimension::equals);
    }
}
