package jp.aquafactory.magitinkerer.machine;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.stln.magitech.content.item.tooltip_item.HintTooltipTextManaContainerBlockItem;
import net.stln.magitech.core.api.mana.ManaCapabilities;

import java.util.function.Supplier;

public final class MenderContent {
    private static final String ID = "mana_durum_mender";
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagiTinkerer.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagiTinkerer.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagiTinkerer.MOD_ID);

    public static final DeferredBlock<ManaDurumMenderBlock> MENDER = BLOCKS.registerBlock(ID, ManaDurumMenderBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 5).noOcclusion());
    public static final DeferredItem<BlockItem> MENDER_ITEM = ITEMS.registerItem(ID,
            properties -> new HintTooltipTextManaContainerBlockItem(MENDER.get(), properties));
    @SuppressWarnings("DataFlowIssue") // この BlockEntity に DataFixer の型は不要。
    public static final Supplier<BlockEntityType<ManaDurumMenderBlockEntity>> MENDER_ENTITY =
            BLOCK_ENTITIES.register(ID,
                    () -> BlockEntityType.Builder.of(ManaDurumMenderBlockEntity::new, MENDER.get()).build(null));

    private MenderContent() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(MenderContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ManaCapabilities.MANA_CONTAINER, MENDER_ENTITY.get(),
                (entity, side) -> entity.getManaFlowRule(entity.getBlockState(), side).isNone()
                        ? null : entity.getManaHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MENDER_ENTITY.get(),
                (entity, side) -> entity.getAutomationHandler());
    }
}
