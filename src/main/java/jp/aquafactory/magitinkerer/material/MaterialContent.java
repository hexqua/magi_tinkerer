package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.machine.MenderContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.stln.magitech.content.block.BlockInit;
import net.stln.magitech.content.item.tooltip_item.TooltipTextBlockItem;
import net.stln.magitech.content.item.tooltip_item.TooltipTextItem;

public final class MaterialContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagiTinkerer.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagiTinkerer.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagiTinkerer.MOD_ID);

    public static final DeferredBlock<TransparentBlock> MANEUVERING_COATED_GLASS = glass("maneuvering_coated_glass");
    public static final DeferredBlock<IronBarsBlock> MANEUVERING_COATED_GLASS_PANE = pane("maneuvering_coated_glass_pane");
    public static final DeferredBlock<TransparentBlock> RESTRAINT_COATED_GLASS = glass("restraint_coated_glass");
    public static final DeferredBlock<IronBarsBlock> RESTRAINT_COATED_GLASS_PANE = pane("restraint_coated_glass_pane");

    public static final DeferredItem<Item> DURUSIUM_INGOT = ITEMS.registerItem("durusium_ingot", TooltipTextItem::new);

    static {
        CREATIVE_TABS.register("materials", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.magitinkerer.materials"))
                .icon(DURUSIUM_INGOT::toStack)
                .displayItems((parameters, output) -> {
                    output.accept(DURUSIUM_INGOT.get());
                    output.accept(MANEUVERING_COATED_GLASS.get());
                    output.accept(MANEUVERING_COATED_GLASS_PANE.get());
                    output.accept(RESTRAINT_COATED_GLASS.get());
                    output.accept(RESTRAINT_COATED_GLASS_PANE.get());
                    output.accept(MenderContent.MENDER_ITEM.get());
                })
                .build());
    }

    private MaterialContent() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    private static DeferredBlock<TransparentBlock> glass(String name) {
        return withItem(BLOCKS.register(name,
                () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                        .sound(BlockInit.CRYSTAL_SOUND))));
    }

    private static DeferredBlock<IronBarsBlock> pane(String name) {
        return withItem(BLOCKS.register(name,
                () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE)
                        .sound(BlockInit.CRYSTAL_SOUND))));
    }

    private static <B extends Block> DeferredBlock<B> withItem(DeferredBlock<B> block) {
        ITEMS.registerItem(block.getId().getPath(), properties -> new TooltipTextBlockItem(block.get(), properties));
        return block;
    }
}
