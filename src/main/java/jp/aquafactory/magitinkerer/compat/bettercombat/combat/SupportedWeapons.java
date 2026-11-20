package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.helper.ComponentHelper;

public final class SupportedWeapons {
    private SupportedWeapons() {}

    public static boolean contains(Item item) {
        return item == ItemInit.LIGHT_SWORD.get() || item == ItemInit.DAGGER.get()
                || item == ItemInit.HEAVY_SWORD.get() || item == ItemInit.SCYTHE.get();
    }

    public static boolean contains(ItemStack stack) {
        return contains(stack.getItem());
    }

    public static boolean invalid(ItemStack stack) {
        return !contains(stack) || ComponentHelper.isBroken(stack)
                || !hasParts(stack);
    }

    public static boolean hasParts(ItemStack stack) {
        return contains(stack) && ComponentHelper.getPartMaterials(stack).size()
                == ((SynthesisedToolItem) stack.getItem()).getToolType().parts().size();
    }

    public static boolean currentAttack(Player player) {
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player,
                ((PlayerAttackProperties) player).getComboCount());
        return hand != null && contains(hand.itemStack());
    }
}

