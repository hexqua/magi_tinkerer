package jp.aquafactory.magitinkerer.compat.bettercombat.client;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackContext;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackEffects;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackRequest;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackRejected;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;

public final class ClientCombat {
    // 接続し直しても連番を戻さず、古い応答を新しい攻撃へ適用しない。
    private static long sequence;

    private ClientCombat() {}

    public static void rejected(AttackRejected rejected) {
        var player = Minecraft.getInstance().player;
        if (player == null || rejected.sequence() != sequence) return;
        ((MinecraftClient_BetterCombat) Minecraft.getInstance()).cancelUpswing();
        ((PlayerAttackProperties) player).setComboCount(rejected.nextCombo());
    }

    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BetterCombatClientEvents.ATTACK_HIT.register((player, hand, targets, cursor) -> {
            if (SupportedWeapons.contains(hand.itemStack())) {
                int combo = ((MinecraftClient_BetterCombat) Minecraft.getInstance()).getComboCount();
                PacketDistributor.sendToServer(new AttackRequest(++sequence, combo, player.getInventory().selected));
            }
        }));
    }

    public static void show(AttackEffects effects) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(effects.playerId()) instanceof Player player)
                || !SupportedWeapons.hasParts(effects.weapon())) return;
        SynthesisedToolItem item = (SynthesisedToolItem) effects.weapon().getItem();
        AttackContext.run(new AttackContext(player, effects.offHand() ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND, effects.weapon(), 1), () -> {
            effects.visual().ifPresent(visual -> SlashRenderer.show(level, visual));
            for (int id : effects.targets()) {
                var target = level.getEntity(id);
                if (target != null) item.callTraitDamageEntity(level, player, target, effects.weapon());
            }
        });
    }
}
