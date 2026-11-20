package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AttackContext {
    public enum Phase { NONE, PHYSICAL, ELEMENTAL, TRAIT }
    // 別プレイヤーや特性の追加攻撃へ、武器本体の倍率を持ち越さない。
    private static final ThreadLocal<AttackContext> ACTIVE = new ThreadLocal<>();

    public final Player player;
    public final InteractionHand hand;
    public final ItemStack weapon;
    public final double multiplier;
    public final List<Integer> targets = new ArrayList<>();
    public AttackGeometry.Box geometry;
    private Phase phase = Phase.NONE;

    public AttackContext(Player player, InteractionHand hand, ItemStack weapon, double multiplier) {
        this.player = player;
        this.hand = hand;
        this.weapon = weapon;
        this.multiplier = multiplier;
    }

    public static AttackContext current(Player player) {
        AttackContext context = ACTIVE.get();
        return context != null && context.player == player ? context : null;
    }

    public static void run(AttackContext context, Runnable action) {
        AttackContext previous = ACTIVE.get();
        ACTIVE.set(context);
        try {
            action.run();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static void phase(Player player, Phase phase, Runnable action) {
        AttackContext context = current(player);
        if (context == null) {
            action.run();
            return;
        }
        Phase previous = context.phase;
        context.phase = phase;
        try {
            action.run();
        } finally {
            context.phase = previous;
        }
    }

    public static boolean fullCharge(Player player) {
        AttackContext context = current(player);
        return context != null && (context.phase == Phase.PHYSICAL || context.phase == Phase.ELEMENTAL);
    }

    public static double scale(Player player, Phase phase, double value) {
        AttackContext context = current(player);
        return context != null && context.phase == phase ? value * context.multiplier : value;
    }

    public static void record(Player player, Entity target) {
        AttackContext context = current(player);
        if (context != null) context.targets.add(target.getId());
    }
}

