package jp.aquafactory.magitinkerer.compat.malum;

import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.trait.TraitHelper;
import net.stln.magitech.feature.tool.trait.TraitInstance;
import net.stln.magitech.helper.ComponentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class MalumSoulhunter {
    private static final ResourceLocation SPOILS = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "soulhunter");
    private static final ThreadLocal<Attack> CURRENT_ATTACK = new ThreadLocal<>();

    private MalumSoulhunter() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MalumSoulhunter::exposeSoul);
    }

    private static void exposeSoul(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0
                && soulhunterLevel(event.getEntity(), event.getSource()) > 0) {
            event.getEntity().getData(MalumAttachmentTypes.LIVING_SOUL_INFO).setExposed();
        }
    }

    // 遅れて着弾した呪文でも、ダメージを実際に発生させた杖を使う。
    public static <T> T withWeapon(Entity target, DamageSource source, @Nullable ItemStack weapon, Supplier<T> damage) {
        Attack previous = CURRENT_ATTACK.get();
        CURRENT_ATTACK.set(new Attack(target, source, weapon));
        try {
            return damage.get();
        } finally {
            if (previous == null) {
                CURRENT_ATTACK.remove();
            } else {
                CURRENT_ATTACK.set(previous);
            }
        }
    }

    public static void withSpoils(LivingEntity target, @Nullable LivingEntity collector, DamageSource source,
                                  Runnable harvest) {
        int bonus = soulhunterLevel(target, source) - 1;
        var spoils = bonus > 0 && collector != null && collector == source.getEntity()
                ? collector.getAttribute(MalumAttributes.SPIRIT_SPOILS) : null;
        if (spoils == null) {
            harvest.run();
            return;
        }

        var previous = spoils.getModifier(SPOILS);
        spoils.removeModifier(SPOILS);
        spoils.addTransientModifier(new AttributeModifier(SPOILS, bonus, AttributeModifier.Operation.ADD_VALUE));
        try {
            harvest.run();
        } finally {
            spoils.removeModifier(SPOILS);
            if (previous != null) {
                spoils.addTransientModifier(previous);
            }
        }
    }

    private static int soulhunterLevel(LivingEntity target, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) {
            return 0;
        }

        Attack attack = CURRENT_ATTACK.get();
        ItemStack weapon;
        if (attack != null && attack.target() == target && attack.source() == source) {
            weapon = attack.weapon();
        } else if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            weapon = source.getWeaponItem();
        } else {
            return 0;
        }

        if (weapon == null || !(weapon.getItem() instanceof SynthesisedToolItem)
                || ComponentHelper.isBroken(weapon)) {
            return 0;
        }
        var trait = ToolMaterials.SOULSTAINED_STEEL.get().getTrait();
        return TraitHelper.getTrait(weapon).stream()
                .filter(instance -> instance.trait() == trait)
                .findFirst().map(TraitInstance::level).orElse(0);
    }

    private record Attack(Entity target, DamageSource source, @Nullable ItemStack weapon) {
    }
}
