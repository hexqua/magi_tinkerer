package jp.aquafactory.magitinkerer.compat.bettercombat.combat;

import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackEffects;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackRequest;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.CombatFlags;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.utils.SoundHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@SuppressWarnings("resource")
public final class CombatServer {
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

    private static final class State {
        long sequence;
        long tick = Long.MIN_VALUE;
        int combo = -1;
        Item main;
        float cooldown;
        int upswingTicks;
    }

    private CombatServer() {}

    public static int nextCombo(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) return 0;
        int next = state.combo == Integer.MAX_VALUE ? 0 : state.combo + 1;
        return resetsCombo(player, state, next) ? 0 : next;
    }

    private static boolean resetsCombo(ServerPlayer player, State state, int requestedCombo) {
        // Better Combat の lastAttacked は命中時ではなく、予備動作の開始時にリセットされる。
        return state.tick == Long.MIN_VALUE
                || player.serverLevel().getGameTime() - state.tick + state.upswingTicks
                > Math.round(state.cooldown * BetterCombatMod.config.combo_reset_rate)
                || state.main != player.getMainHandItem().getItem()
                && !PlayerAttackHelper.shouldAttackWithOffHand(player, requestedCombo);
    }

    public static Optional<AttackEffects> attack(ServerPlayer player, AttackRequest request) {
        if (request.sequence() <= 0 || request.combo() < 0
                || request.slot() != player.getInventory().selected
                || !player.isAlive() || player.isSpectator() || player.isUsingItem()
                || CombatFlags.isAttackDisabled(player)) return Optional.empty();
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, request.combo());
        if (hand == null || SupportedWeapons.invalid(hand.itemStack())) return Optional.empty();
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        if (request.sequence() <= state.sequence) return Optional.empty();
        long now = player.serverLevel().getGameTime();
        boolean reset = resetsCombo(player, state, request.combo());
        if (request.combo() != (reset ? 0 : state.combo + 1)) return Optional.empty();

        Item originalMain = player.getMainHandItem().getItem();
        return HandScope.call(player, hand.isOffHand(), request.combo(), () -> {
            float cooldown = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
            // 通信の tick 境界だけを許容し、最小攻撃間隔は短縮しない。
            int interval = Math.max(BetterCombatMod.config.attack_interval_cap,
                    (int) Math.ceil(cooldown) - 1);
            if (!Float.isFinite(cooldown) || cooldown <= 0
                    || state.tick != Long.MIN_VALUE && now - state.tick < interval) return Optional.empty();
            if (state.tick == Long.MIN_VALUE
                    && player.getAttackStrengthScale(0) < 1 - hand.upswingRate()) return Optional.empty();
            double multiplier = Math.max(0, hand.attack().damageMultiplier()
                    + PlayerAttackHelper.getDualWieldingAttackDamageMultiplier(player, hand) - 1);
            if (!Double.isFinite(multiplier)) return Optional.empty();

            // 特性からの再入や同じ packet の再送でも二回実行しない。
            state.sequence = request.sequence();
            state.tick = now;
            state.combo = request.combo();
            state.main = originalMain;
            state.cooldown = cooldown;
            state.upswingTicks = Math.max(1, Math.round(cooldown * (float) hand.upswingRate()));
            InteractionHand usedHand = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            var item = (SynthesisedToolItem) hand.itemStack().getItem();
            var properties = item.getAppliedProperties(player, player.level(), hand.itemStack());
            var elemental = ToolPropertyInit.ELEMENTAL_DAMAGE.get();
            var element = elemental.getElement(properties.get(elemental));
            var rolls = SlashVisual.resolveRolls(hand.attack(), hand.isOffHand(),
                    player.getMainArm() == HumanoidArm.LEFT, player.getRandom());
            AttackContext context = new AttackContext(player, usedHand, hand.itemStack(), multiplier);
            AttackContext.run(context, () -> ((SynthesisedToolItem) hand.itemStack().getItem())
                    .sweepAttack(player.level(), InteractionHand.MAIN_HAND, player));
            SoundHelper.playSound(player.serverLevel(), player, hand.attack().swingSound());
            return Optional.of(new AttackEffects(player.getId(),
                    hand.isOffHand(), hand.itemStack().copy(),
                    context.targets.stream().mapToInt(Integer::intValue).toArray(),
                    context.geometry == null ? Optional.empty() : Optional.of(SlashVisual.from(context.geometry,
                            rolls, element.getPrimary().getRGB(), element.getSecondary().getRGB()))));
        });
    }

    public static void observeOrdinaryAttack(ServerPlayer player, int combo, AttackHand hand) {
        if (combo < 0 || hand == null) return;
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.tick = player.serverLevel().getGameTime();
        state.combo = combo;
        state.main = player.getMainHandItem().getItem();
        state.cooldown = HandScope.call(player, hand.isOffHand(), combo,
                () -> PlayerAttackHelper.getAttackCooldownTicksCapped(player));
        state.upswingTicks = Math.max(1, Math.round(state.cooldown * (float) hand.upswingRate()));
    }
}
