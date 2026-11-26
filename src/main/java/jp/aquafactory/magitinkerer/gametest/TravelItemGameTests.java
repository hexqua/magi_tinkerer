package jp.aquafactory.magitinkerer.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.machine.ManaDurumMenderBlockEntity;
import jp.aquafactory.magitinkerer.machine.MenderContent;
import jp.aquafactory.magitinkerer.magicitem.TravelItem;
import jp.aquafactory.magitinkerer.magicitem.TravelItemContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.core.api.mana.ManaCapabilities;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.data.DataAttachmentInit;
import net.stln.magitech.feature.magic.cooldown.CooldownHelper;
import net.stln.magitech.helper.ComponentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(MagiTinkerer.MOD_ID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class TravelItemGameTests {
    @GameTest(template = "gametest/runtime")
    public static void mirrorsChargeAtTheirFlowAndCanBeExtractedWhenFull(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MenderContent.MENDER.get());
        ManaDurumMenderBlockEntity mender = helper.getBlockEntity(pos);
        var automation = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(pos), Direction.EAST);
        if (automation == null) {
            helper.fail("The mender must expose automation");
            return;
        }
        helper.assertTrue(!automation.insertItem(0, TravelItemContent.SIGNUM_THREAD.toStack(), false).isEmpty(),
                "The thread must not charge in the mender");

        var mirror = TravelItemContent.SPATIAL_MIRROR.toStack();
        ComponentHelper.updateMana(mirror, mana -> 291_500L);
        helper.assertTrue(automation.insertItem(0, mirror, false).isEmpty(), "The mender must accept the spatial mirror");
        mender.setMana(6_500);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ComponentHelper.getMana(mender.getItem(0)) >= 296_500,
                        "The mirror must begin charging"))
                .thenExecute(() -> {
                    helper.assertTrue(ComponentHelper.getMana(mender.getItem(0)) == 296_500 && mender.getMana() == 1_500,
                            "One tick must transfer no more than 5 kJ");
                    helper.assertTrue(automation.extractItem(0, 1, true).isEmpty()
                            && automation.extractItem(0, 1, false).isEmpty(), "A partially charged mirror must stay inside");
                })
                .thenWaitUntil(() -> helper.assertTrue(mender.getMana() == 0, "The mender must supply its last 1.5 kJ"))
                .thenExecute(() -> {
                    helper.assertTrue(ComponentHelper.getMana(mender.getItem(0)) == 298_000, "Partial charging must conserve mana");
                    mender.setMana(8_000);
                })
                .thenWaitUntil(() -> helper.assertTrue(!automation.extractItem(0, 1, true).isEmpty(),
                        "The charged mirror must be available for extraction"))
                .thenExecute(() -> {
                    var charged = automation.extractItem(0, 1, false);
                    helper.assertTrue(charged.is(mirror.getItem()) && ComponentHelper.getMana(charged) == 300_000
                            && mender.getMana() == 6_000, "Charging must stop at capacity and preserve the remaining mana");
                    helper.assertTrue(automation.insertItem(0, TravelItemContent.DIMENSIONAL_MIRROR.toStack(), false).isEmpty(),
                            "The mender must also accept the dimensional mirror");
                })
                .thenSucceed();
    }

    @GameTest(template = "gametest/runtime")
    public static void threadAbsorptionRespectsIntervalAndBothBalances(GameTestHelper helper) {
        var level = helper.getLevel();
        long delay = Math.floorMod(-level.getGameTime(), 10);
        helper.runAfterDelay(delay + 1, () -> withPlayer(helper, player -> {
            var item = TravelItemContent.SIGNUM_THREAD.get();
            var stack = item.getDefaultInstance();
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            var mana = player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY);
            if (mana == null) {
                helper.fail("The player must expose mana");
                return;
            }
            mana.setMana(7_500);
            item.inventoryTick(stack, level, player, 0, false);
            helper.assertTrue(mana.getMana() == 7_500 && ComponentHelper.getMana(stack) == 0,
                    "The thread must not absorb mana between interval ticks");
        }));
        helper.runAfterDelay(delay + 10, () -> withPlayer(helper, player -> {
            var stack = TravelItemContent.SIGNUM_THREAD.toStack();
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            var mana = player.getCapability(ManaCapabilities.MANA_CAPABLE_ENTITY);
            if (mana == null) {
                helper.fail("The player must expose mana");
                return;
            }
            mana.setMana(7_500);
            player.getInventory().tick();
            helper.assertTrue(mana.getMana() == 0 && ComponentHelper.getMana(stack) == 7_500,
                    "The offhand thread must only absorb the available mana");
            ComponentHelper.updateMana(stack, value -> 99_250L);
            mana.setMana(12_000);
            player.getInventory().tick();
            helper.assertTrue(mana.getMana() == 11_250 && ComponentHelper.getMana(stack) == 100_000,
                    "Only the remaining capacity may be drawn from the player");
            helper.succeed();
        }));
    }

    @GameTest(template = "gametest/runtime")
    public static void travelOnlySpendsManaAndStartsCooldownAfterSuccess(GameTestHelper helper) {
        withPlayer(helper, player -> {
            var level = helper.getLevel();
            BlockPos start = new BlockPos(1, 1, 1);
            BlockPos roof = start.above(3);
            helper.setBlock(start.below(), Blocks.STONE);
            helper.setBlock(start, Blocks.AIR);
            helper.setBlock(start.above(), Blocks.AIR);
            helper.setBlock(roof, Blocks.MAGMA_BLOCK);
            Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(start));
            player.setPos(origin);

            var thread = TravelItemContent.SIGNUM_THREAD.get();
            var stack = thread.getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(thread.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                    "An empty thread must reject casting");
            ComponentHelper.updateMana(stack, mana -> 100_000L);
            thread.use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.isUsingItem(), "A charged thread must begin casting");
            player.releaseUsingItem();
            helper.assertTrue(ComponentHelper.getMana(stack) == 100_000 && player.position().equals(origin)
                    && !CooldownHelper.isCooldown(player, thread.spell()) && !player.getCooldowns().isOnCooldown(thread),
                    "Releasing early must not move the player, cost mana or start cooldowns");
            long cost = thread.manaCost(player);
            thread.use(level, player, InteractionHand.MAIN_HAND);
            completeUse(thread, stack, player);
            helper.assertTrue(player.position().distanceToSqr(Vec3.atBottomCenterOf(helper.absolutePos(roof.above()))) < 0.01
                    && ComponentHelper.getMana(stack) == 100_000 - cost,
                    "The thread must land above the magma block like Luxgrail and spend the item mana once");
            helper.assertTrue(CooldownHelper.isCooldown(player, thread.spell()) && player.getCooldowns().isOnCooldown(thread),
                    "Success must start both thread cooldowns");

            var mirror = TravelItemContent.SPATIAL_MIRROR.get();
            var mirrorStack = mirror.getDefaultInstance();
            ComponentHelper.updateMana(mirrorStack, mana -> 300_000L);
            player.setItemInHand(InteractionHand.MAIN_HAND, mirrorStack);
            BlockPos spawn = helper.absolutePos(start);
            player.setRespawnPosition(level.dimension(), spawn, 0, true, false);
            var target = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
            helper.assertTrue(!target.missingRespawnBlock() && BlockPos.containing(target.pos()).equals(spawn),
                    "The fixture must provide a valid forced spawn");
            long mirrorCost = mirror.manaCost(player);
            helper.assertTrue(mirror.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME
                    && player.isUsingItem(), "The mirror must begin casting with a valid same-dimension spawn");
            completeUse(mirror, mirrorStack, player);
            helper.assertTrue(player.position().distanceToSqr(target.pos()) < 0.01,
                    "The mirror must return to the resolved respawn point (expected " + target.pos()
                            + ", actual " + player.position() + ")");
            helper.assertTrue(ComponentHelper.getMana(mirrorStack) == 300_000 - mirrorCost,
                    "The mirror must spend item mana exactly once");
            var other = TravelItemContent.DIMENSIONAL_MIRROR.get();
            var otherStack = other.getDefaultInstance();
            ComponentHelper.updateMana(otherStack, mana -> 300_000L);
            player.setItemInHand(InteractionHand.MAIN_HAND, otherStack);
            helper.assertTrue(other.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL
                    && !player.isUsingItem() && ComponentHelper.getMana(otherStack) == 300_000
                    && !player.getCooldowns().isOnCooldown(other), "The second mirror must reject the cast during the shared Tenebport cooldown");
            player.getData(DataAttachmentInit.SPELL_COOLDOWNS.get()).remove(other.spell());
            helper.assertTrue(other.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME
                    && player.isUsingItem(), "The second mirror must cast when its own item and spell cooldowns have expired");
            completeUse(other, otherStack, player);

            var itemCooldowns = player.getCooldowns();
            for (int tick = 0; tick < 199; tick++) {
                itemCooldowns.tick();
            }
            helper.assertTrue(itemCooldowns.isOnCooldown(thread) && itemCooldowns.isOnCooldown(mirror)
                    && itemCooldowns.isOnCooldown(other), "Each travel item must still be cooling down after 199 ticks");
            itemCooldowns.tick();
            helper.assertTrue(!itemCooldowns.isOnCooldown(thread) && !itemCooldowns.isOnCooldown(mirror)
                    && !itemCooldowns.isOnCooldown(other), "Each travel item cooldown must expire after 200 ticks");
            helper.succeed();
        });
    }

    private static void completeUse(TravelItem item, ItemStack stack, ServerPlayer player) {
        int duration = (int) MagicPerformanceHelper.getEffectiveChargeTime(player, TravelItem.heldWand(player), item.spell());
        item.onUseTick(player.level(), player, stack, item.getUseDuration(stack, player) - duration + 1);
    }

    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> action) {
        var profile = new GameProfile(UUID.randomUUID(), "travel-test");
        ServerPlayer player = new FakePlayer(helper.getLevel(), profile);
        // FakePlayer 標準の接続は転送も無視するため、位置更新を行う通常のリスナーに差し替える。
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(@NotNull Packet<?> packet, @Nullable PacketSendListener listener) {
                // テストはサーバー上の転送だけを検証する。
            }
        };
        try {
            action.accept(player);
        } finally {
            player.releaseUsingItem();
        }
    }
}
