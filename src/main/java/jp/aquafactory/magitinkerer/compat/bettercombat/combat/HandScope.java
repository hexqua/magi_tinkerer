package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.function.Supplier;

public final class HandScope {
    private HandScope() {}

    public static <T> T call(Player player, boolean offHand, int combo, Supplier<T> action) {
        PlayerAttackProperties properties = (PlayerAttackProperties) player;
        int previousCombo = properties.getComboCount();
        int selected = player.getInventory().selected;
        ItemStack main = player.getInventory().getSelected();
        ItemStack off = player.getInventory().offhand.getFirst();
        List<ItemAttributeModifiers.Entry> mainModifiers = modifiers(main);
        List<ItemAttributeModifiers.Entry> offModifiers = modifiers(off);
        try {
            properties.setComboCount(combo);
            if (offHand) {
                remove(player, mainModifiers);
                add(player, offModifiers);
                // Magitech と vanilla のメインハンド参照にも、攻撃中の武器を見せる。
                player.getInventory().items.set(selected, off);
            }
            return action.get();
        } finally {
            if (offHand) {
                remove(player, offModifiers);
                player.getInventory().items.set(selected, main);
                player.getInventory().offhand.set(0, off);
                add(player, mainModifiers);
            }
            player.getInventory().selected = selected;
            properties.setComboCount(previousCombo);
        }
    }

    private static List<ItemAttributeModifiers.Entry> modifiers(ItemStack stack) {
        return stack.getAttributeModifiers().modifiers().stream()
                .filter(entry -> entry.slot().test(EquipmentSlot.MAINHAND)).toList();
    }

    private static void remove(Player player, List<ItemAttributeModifiers.Entry> modifiers) {
        for (ItemAttributeModifiers.Entry entry : modifiers) {
            var instance = player.getAttribute(entry.attribute());
            if (instance != null) instance.removeModifier(entry.modifier().id());
        }
    }

    private static void add(Player player, List<ItemAttributeModifiers.Entry> modifiers) {
        for (ItemAttributeModifiers.Entry entry : modifiers) {
            Holder<Attribute> attribute = entry.attribute();
            AttributeModifier modifier = entry.modifier();
            var instance = player.getAttribute(attribute);
            if (instance != null) instance.addOrUpdateTransientModifier(modifier);
        }
    }
}

