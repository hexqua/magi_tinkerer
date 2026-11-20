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
import net.stln.magitech.feature.tool.property.ToolPropertyCategory;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.tool_category.ToolCategoryInit;
import net.stln.magitech.feature.tool.trait.Trait;
import net.stln.magitech.registry.DeferredToolMaterial;
import net.stln.magitech.registry.DeferredToolMaterialRegister;
import net.stln.magitech.registry.DeferredTrait;
import net.stln.magitech.registry.DeferredTraitRegister;

import java.awt.Color;

public final class ToolMaterials {
    public static final ResourceLocation SOULSTAINED_STEEL_INGOT =
            ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MALUM_MOD_ID, "soul_stained_steel_ingot");
    public static final TagKey<Item> SOULSTAINED_STEEL_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "soulstained_steel_ingots"));

    private static final DeferredTraitRegister TRAITS = new DeferredTraitRegister(MagiTinkerer.MOD_ID);
    private static final DeferredToolMaterialRegister MATERIALS = new DeferredToolMaterialRegister(MagiTinkerer.MOD_ID);

    private static final DeferredTrait<Trait> SEA_FRIENDS = TRAITS.register("sea_friends", SeaFriendsTrait::new);
    private static final DeferredTrait<Trait> SOULHUNTER = TRAITS.register("soulhunter", SoulhunterTrait::new);
    private static final DeferredTrait<Trait> HOLLOW = TRAITS.register("hollow",
            () -> new CoatedGlassTrait("hollow", new Color(0x9BCBD5), ToolPropertyCategory.HANDLING,
                    ToolPropertyCategory.CONTINUITY, ToolPropertyCategory.RANGE, ToolPropertyCategory.UNIQUE));
    private static final DeferredTrait<Trait> INTENSIFY = TRAITS.register("intensify",
            () -> new CoatedGlassTrait("intensify", new Color(0xC7AED7), ToolPropertyCategory.ATTACK,
                    ToolPropertyCategory.ELEMENT));
    private static final DeferredTrait<Trait> STEADFAST = TRAITS.register("steadfast", SteadfastTrait::new);

    public static final DeferredToolMaterial<ToolMaterial> MANEUVERING_COATED_GLASS =
            MATERIALS.register("maneuvering_coated_glass", () -> new ToolMaterial(MaterialInit.GLASS.get().order() + 0.1F,
                    ToolMaterials::maneuveringGlassProperties, HOLLOW));
    public static final DeferredToolMaterial<ToolMaterial> RESTRAINT_COATED_GLASS =
            MATERIALS.register("restraint_coated_glass", () -> new ToolMaterial(MaterialInit.GLASS.get().order() + 0.2F,
                    ToolMaterials::restraintGlassProperties, INTENSIFY));
    public static final DeferredToolMaterial<ToolMaterial> DURUSIUM =
            MATERIALS.register("durusium", () -> new ToolMaterial(MaterialInit.FLUXIUM.get().order() + 0.1F,
                    ToolMaterials::durusiumProperties, STEADFAST));

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

    private static ToolProperties maneuveringGlassProperties() {
        return coatedGlassProperties()
                .set(ToolPropertyInit.ATTACK_SPEED.get(), 1.10)
                .set(ToolPropertyInit.MINING_SPEED.get(), 1.05)
                .set(ToolPropertyInit.REACH.get(), 1.05)
                .set(ToolPropertyInit.SWEEP.get(), 1.05)
                .set(ToolPropertyInit.CHARGE_SPEED.get(), 1.10)
                .set(ToolPropertyInit.COOLDOWN_SPEED.get(), 1.05)
                .set(ToolPropertyInit.LAUNCH.get(), 1.05)
                .set(ToolPropertyInit.MANA_EFFICIENCY.get(), 1.05);
    }

    private static ToolProperties restraintGlassProperties() {
        return coatedGlassProperties()
                .set(ToolPropertyInit.DAMAGE.get(), 1.10)
                .set(ToolPropertyInit.POWER.get(), 1.10);
    }

    private static ToolProperties coatedGlassProperties() {
        return new ToolProperties(ToolCategoryInit.ALL)
                .set(ToolPropertyInit.TIER.get(), 0.0)
                .set(ToolPropertyInit.MAX_PROGRESSION_COEFFICIENT.get(), 1.0)
                .set(ToolPropertyInit.DAMAGE.get(), 1.0)
                .set(ToolPropertyInit.ELEMENTAL_DAMAGE.get(), ElementalAttributeToolProperty.none())
                .set(ToolPropertyInit.ATTACK_SPEED.get(), 1.0)
                .set(ToolPropertyInit.MINING_SPEED.get(), 1.0)
                .set(ToolPropertyInit.DEFENSE.get(), 0.0)
                .set(ToolPropertyInit.REACH.get(), 1.0)
                .set(ToolPropertyInit.SWEEP.get(), 1.0)
                .set(ToolPropertyInit.DURABILITY.get(), 0.70)
                .set(ToolPropertyInit.MINING_LEVEL.get(), MiningLevel.NONE)
                .set(ToolPropertyInit.POWER.get(), 1.0)
                .set(ToolPropertyInit.ELEMENTAL_POWER.get(), ElementalAttributeToolProperty.none())
                .set(ToolPropertyInit.CHARGE_SPEED.get(), 1.0)
                .set(ToolPropertyInit.COOLDOWN_SPEED.get(), 1.0)
                .set(ToolPropertyInit.LAUNCH.get(), 1.0)
                .set(ToolPropertyInit.MANA_EFFICIENCY.get(), 1.0);
    }

    private static ToolProperties durusiumProperties() {
        var elemental = ElementalAttributeToolProperty.singleElement(Element.MANA, 0.10);
        return new ToolProperties(ToolCategoryInit.ALL)
                .set(ToolPropertyInit.TIER.get(), 2.0)
                .set(ToolPropertyInit.MAX_PROGRESSION_COEFFICIENT.get(), 1.0)
                .set(ToolPropertyInit.DAMAGE.get(), 1.10)
                .set(ToolPropertyInit.ELEMENTAL_DAMAGE.get(), elemental)
                .set(ToolPropertyInit.ATTACK_SPEED.get(), 0.80)
                .set(ToolPropertyInit.MINING_SPEED.get(), 0.85)
                .set(ToolPropertyInit.DEFENSE.get(), 1.10)
                .set(ToolPropertyInit.REACH.get(), 1.10)
                .set(ToolPropertyInit.SWEEP.get(), 1.05)
                .set(ToolPropertyInit.DURABILITY.get(), 1.50)
                .set(ToolPropertyInit.MINING_LEVEL.get(), MiningLevel.IRON)
                .set(ToolPropertyInit.POWER.get(), 1.10)
                .set(ToolPropertyInit.ELEMENTAL_POWER.get(), elemental)
                .set(ToolPropertyInit.CHARGE_SPEED.get(), 0.80)
                .set(ToolPropertyInit.COOLDOWN_SPEED.get(), 0.85)
                .set(ToolPropertyInit.LAUNCH.get(), 1.10)
                .set(ToolPropertyInit.MANA_EFFICIENCY.get(), 1.05);
    }

    public static void register(IEventBus modBus) {
        TRAITS.register(modBus);
        MATERIALS.register(modBus);
    }
}
