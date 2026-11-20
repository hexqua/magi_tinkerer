package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.material.MiningLevel;
import net.stln.magitech.feature.tool.material.ToolMaterial;
import net.stln.magitech.feature.tool.property.ElementalAttributeToolProperty;
import net.stln.magitech.feature.tool.property.ToolProperties;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.tool_category.ToolCategoryInit;
import net.stln.magitech.feature.tool.trait.Trait;
import net.stln.magitech.registry.DeferredToolMaterial;
import net.stln.magitech.registry.DeferredToolMaterialRegister;
import net.stln.magitech.registry.DeferredTrait;
import net.stln.magitech.registry.DeferredTraitRegister;

public final class ToolMaterials {
    public static final ResourceLocation SOULSTAINED_STEEL_INGOT =
            ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MALUM_MOD_ID, "soul_stained_steel_ingot");
    public static final TagKey<Item> SOULSTAINED_STEEL_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "soulstained_steel_ingots"));

    private static final DeferredTraitRegister TRAITS = new DeferredTraitRegister(MagiTinkerer.MOD_ID);
    private static final DeferredToolMaterialRegister MATERIALS = new DeferredToolMaterialRegister(MagiTinkerer.MOD_ID);

    private static final DeferredTrait<Trait> SEA_FRIENDS = TRAITS.register("sea_friends", SeaFriendsTrait::new);
    private static final DeferredTrait<Trait> SOULHUNTER = TRAITS.register("soulhunter", SoulhunterTrait::new);

    public static final DeferredToolMaterial<ToolMaterial> PRISMARINE_CRYSTAL =
            MATERIALS.register("prismarine_crystal", () -> new ToolMaterial(
                            MaterialInit.SAMPLE.get().order(), // todo:それっぽい値にする.
                            () -> MaterialInit.SAMPLE.get().properties().get().copy(), // todo:それっぽい値にする.
                            SEA_FRIENDS
                    )
            );

    // 保存済みの装備を復元できるよう、Malum 不在時も同じ ID を登録する。
    public static final DeferredToolMaterial<ToolMaterial> SOULSTAINED_STEEL =
            MATERIALS.register("soulstained_steel", () -> new ToolMaterial(MaterialInit.OBSIDIAN.get().order(),
                    () -> new ToolProperties(ToolCategoryInit.ALL)
                            .set(ToolPropertyInit.TIER.get(), 2.0)
                            .set(ToolPropertyInit.MAX_PROGRESSION_COEFFICIENT.get(), 1.0)
                            .set(ToolPropertyInit.DAMAGE.get(), 0.5)
                            .set(ToolPropertyInit.ELEMENTAL_DAMAGE.get(), ElementalAttributeToolProperty.singleElement(Element.MAGIC, 1.2))
                            .set(ToolPropertyInit.ATTACK_SPEED.get(), 0.7)
                            .set(ToolPropertyInit.MINING_SPEED.get(), 0.8)
                            .set(ToolPropertyInit.DEFENSE.get(), 1.2)
                            .set(ToolPropertyInit.REACH.get(), 0.9)
                            .set(ToolPropertyInit.SWEEP.get(), 1.1)
                            .set(ToolPropertyInit.DURABILITY.get(), 2.0)
                            .set(ToolPropertyInit.MINING_LEVEL.get(), MiningLevel.DIAMOND)
                            .set(ToolPropertyInit.POWER.get(), 1.0)
                            .set(ToolPropertyInit.ELEMENTAL_POWER.get(), ElementalAttributeToolProperty.singleElement(Element.MAGIC, 1.2))
                            .set(ToolPropertyInit.CHARGE_SPEED.get(), 0.7)
                            .set(ToolPropertyInit.COOLDOWN_SPEED.get(), 0.8)
                            .set(ToolPropertyInit.LAUNCH.get(), 0.6)
                            .set(ToolPropertyInit.MANA_EFFICIENCY.get(), 1.3), SOULHUNTER));

    private ToolMaterials() {
    }

    public static void register(IEventBus modBus) {
        TRAITS.register(modBus);
        MATERIALS.register(modBus);
    }
}
