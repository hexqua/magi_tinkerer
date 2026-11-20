package jp.aquafactory.magitinkerer.gametest.bettercombat;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackContext;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.CombatServer;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.HandScope;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SlashVisual;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackRequest;
import jp.aquafactory.magitinkerer.compat.bettercombat.network.AttackEffects;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.component.BetterCombatDataComponents;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.stln.magitech.content.entity.AdjustableAttackStrengthEntity;
import net.stln.magitech.content.entity.status.AttributeInit;
import net.stln.magitech.content.item.component.ComponentInit;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.material.ToolMaterialLike;
import net.stln.magitech.feature.tool.property.ToolPropertyInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeLike;
import net.stln.magitech.helper.ComponentHelper;
import net.stln.magitech.helper.CombatHelper;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class WeaponCombatGameTests {
    private WeaponCombatGameTests() {}

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void allFourWeaponsUseMagitechDamageAndConsumeOnce(GameTestHelper helper) {
        for (ToolTypeLike type : List.of(ToolTypeInit.LIGHT_SWORD, ToolTypeInit.DAGGER,
                ToolTypeInit.HEAVY_SWORD, ToolTypeInit.SCYTHE)) {
            FakePlayer player = player(helper);
            ItemStack weapon = tool(player, type, MaterialInit.SAMPLE);
            equip(player, weapon, ItemStack.EMPTY);
            var attributes = Objects.requireNonNull(WeaponRegistry.getAttributes(weapon),
                    "Synthesised weapon has no Better Combat attributes");
            helper.assertTrue(attributes.isTwoHanded() == type.asToolType().heavyTool(),
                    "Incorrect handedness");
            helper.assertTrue(attributes.attacks().length == 2,
                    "Parent preset left an unwanted finisher in the combo");
            for (var attack : attributes.attacks()) {
                helper.assertTrue(attack.animation().contains("slash_horizontal_")
                                && attack.damageMultiplier() == 1,
                        "Default attack is not a horizontal slash with neutral damage");
            }
            Cow target = target(helper, 2.5F, 3);
            AttackHand hand = Objects.requireNonNull(PlayerAttackHelper.getCurrentAttack(player, 0));
            double expected = damage(player) * hand.attack().damageMultiplier();
            // 発動可能だが未充填の状態でも、連携攻撃の威力は減衰させない。
            ticker(player, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay() * 0.8));
            int progression = ComponentHelper.getProgression(weapon);
            var result = CombatServer.attack(player, new AttackRequest(1, 0, 0));
            helper.assertTrue(result.isPresent(), "First attack was rejected");
            close(helper, 200 - target.getHealth(), expected, "Physical and elemental damage; player="
                    + player.position() + ", target=" + target.position() + ", center="
                    + CombatHelper.getAttackTargetPosition(player, player.entityInteractionRange(), 1, 0)
                    + ", reach=" + player.entityInteractionRange() + ", hits=" + result.orElseThrow().targets().length);
            helper.assertTrue(weapon.getDamageValue() == 1, "Durability was not consumed once");
            helper.assertTrue(ComponentHelper.getProgression(weapon) == progression + 1,
                    "Progression was not advanced once");
            helper.assertTrue(result.orElseThrow().targets().length == 1, "Incorrect visual hit targets");
            helper.assertTrue(AttackContext.current(player) == null, "Attack context leaked");
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void oldAndNormalRequestsCannotDuplicateCustomAttack(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, ItemStack.EMPTY);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        InteractionResult click = ((SynthesisedToolItem) weapon.getItem())
                .onLeftClick(player, InteractionHand.MAIN_HAND, player.level());
        helper.assertTrue(click == InteractionResult.PASS && target.getHealth() == 200,
                "Old left click still attacks");
        for (int combo : new int[]{0, -1}) {
            ServerNetwork.handleAttackRequest(new Packets.C2S_AttackRequest(combo, false, 0, target, List.of(target)),
                    player.server, player, player.connection);
        }
        helper.assertTrue(target.getHealth() == 200 && weapon.getDamageValue() == 0,
                "Normal Better Combat request still attacks");
        player.attack(target);
        helper.assertTrue(target.getHealth() == 200 && weapon.getDamageValue() == 0,
                "Vanilla attack bypassed the custom route");
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(),
                "Custom request was rejected");
        float health = target.getHealth();
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(),
                "Duplicate sequence was accepted");
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(2, 1, 0)).isEmpty(),
                "Attack interval was bypassed");
        helper.assertTrue(target.getHealth() == health && weapon.getDamageValue() == 1,
                "Rejected request changed combat state");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor", timeoutTicks = 80)
    public static void offHandUsesItsOwnAttributesAndRestoresInventory(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack main = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        ItemStack off = tool(player, ToolTypeInit.DAGGER, MaterialInit.WOOD);
        equip(player, main, off);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(),
                "Main hand attack was rejected");
        double mainDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double mainSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double expected = HandScope.call(player, true, 1, () -> damage(player)
                * Math.max(0, Objects.requireNonNull(PlayerAttackHelper.getCurrentAttack(player, 1)).attack().damageMultiplier()
                + BetterCombatMod.config.dual_wielding_off_hand_damage_multiplier - 1));
        helper.runAfterDelay(12, () -> {
            target.moveTo(helper.absoluteVec(new Vec3(2.5, 1, 3)));
            target.setHealth(200);
            target.invulnerableTime = 0;
            helper.assertTrue(CombatServer.attack(player, new AttackRequest(2, 1, 0)).isPresent(),
                    "Offhand attack was rejected");
            close(helper, 200 - target.getHealth(), expected, "Offhand physical and elemental damage");
            helper.assertTrue(main.getDamageValue() == 1 && off.getDamageValue() == 1,
                    "Durability was applied to the wrong hand");
            helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off,
                    "Hand inventory was not restored");
            close(helper, player.getAttributeValue(Attributes.ATTACK_DAMAGE), mainDamage, "Restored damage");
            close(helper, player.getAttributeValue(Attributes.ATTACK_SPEED), mainSpeed, "Restored speed");
            helper.assertTrue(((PlayerAttackProperties) player).getComboCount() == -1, "Combo context leaked");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void scopeRestoresAfterTraitException(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack main = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        ItemStack off = tool(player, ToolTypeInit.DAGGER, MaterialInit.WOOD);
        equip(player, main, off);
        double before = damage(player);
        try {
            AttackContext.run(new AttackContext(player, InteractionHand.OFF_HAND, off, 2),
                    () -> HandScope.call(player, true, 1, () -> {
                        throw new IllegalStateException("Intentional trait failure");
                    }));
            throw new AssertionError("Expected exception");
        } catch (IllegalStateException expected) {
            helper.assertTrue("Intentional trait failure".equals(expected.getMessage()), "Unexpected exception");
        }
        close(helper, damage(player), before, "Attributes after exception");
        helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off,
                "Inventory leaked after exception");
        helper.assertTrue(AttackContext.current(player) == null
                && ((PlayerAttackProperties) player).getComboCount() == -1, "Context leaked after exception");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void invalidEquipmentAndPacketsDoNotAttack(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, ItemStack.EMPTY);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        for (AttackRequest request : List.of(new AttackRequest(0, 0, 0), new AttackRequest(1, -1, 0),
                new AttackRequest(1, 2, 0), new AttackRequest(1, 0, 1))) {
            helper.assertTrue(CombatServer.attack(player, request).isEmpty(), "Invalid request accepted: " + request);
        }
        weapon.set(ComponentInit.BROKEN_COMPONENT, true);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(), "Broken weapon accepted");
        weapon.set(ComponentInit.BROKEN_COMPONENT, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(), "Switched weapon accepted");
        player.setItemInHand(InteractionHand.MAIN_HAND, tool(player, ToolTypeInit.HEAVY_SWORD, MaterialInit.SAMPLE));
        player.setItemInHand(InteractionHand.OFF_HAND, weapon);
        helper.assertTrue(!PlayerAttackHelper.isDualWielding(player), "Two-handed weapon allowed dual wielding");
        helper.assertTrue(target.getHealth() == 200 && weapon.getDamageValue() == 0, "Rejected input caused damage");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void magitechAreaHitsMultipleTargetsWithoutBetterCombatFalloff(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, ItemStack.EMPTY);
        Cow first = target(helper, 2.5F, 3);
        Cow second = target(helper, 3.1F, 3);
        Cow far = target(helper, 2.5F, 8);
        ticker(player, 100);
        double expected = damage(player);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(), "Area attack rejected");
        close(helper, 200 - first.getHealth(), expected, "First area target");
        close(helper, 200 - second.getHealth(), expected, "Second area target without falloff");
        helper.assertTrue(far.getHealth() == 200, "Out-of-range target was damaged");
        helper.assertTrue(weapon.getDamageValue() == 1, "Area damage consumed durability per target");
        far.discard();
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void wallsAndEmptySwingsRespectMagitechRules(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, ItemStack.EMPTY);
        Cow target = target(helper, 2.5F, 3);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        ticker(player, 100);
        var result = CombatServer.attack(player, new AttackRequest(1, 0, 0));
        helper.assertTrue(result.isPresent(), "Swing without Better Combat targets was rejected");
        helper.assertTrue(target.getHealth() == 200 && weapon.getDamageValue() == 0,
                "Wall did not block Magitech damage");
        helper.assertTrue(result.orElseThrow().targets().length == 0, "Empty swing reported a hit");
        helper.succeed();
    }


    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void dualWieldConfigAndTraitPhaseAreAppliedSeparately(GameTestHelper helper) {
        float speed = BetterCombatMod.config.dual_wielding_attack_speed_multiplier;
        float damage = BetterCombatMod.config.dual_wielding_main_hand_damage_multiplier;
        try {
            BetterCombatMod.config.dual_wielding_attack_speed_multiplier = 1.5F;
            BetterCombatMod.config.dual_wielding_main_hand_damage_multiplier = 1.4F;
            FakePlayer player = player(helper);
            ItemStack main = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
            equip(player, main, tool(player, ToolTypeInit.DAGGER, MaterialInit.SAMPLE));
            double baseSpeed = ((SynthesisedToolItem) main.getItem())
                    .getAppliedProperties(player, player.level(), main)
                    .getOrId(ToolPropertyInit.ATTACK_SPEED.get());
            close(helper, player.getAttributeValue(Attributes.ATTACK_SPEED), baseSpeed * 1.5, "Dual wield speed");
            Cow target = target(helper, 2.5F, 3);
            ticker(player, 100);
            double expected = damage(player) * 1.4;
            helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(),
                    "Configured dual wield attack rejected");
            close(helper, 200 - target.getHealth(), expected, "Configured dual wield damage");
            target.setHealth(200);
            target.invulnerableTime = 0;
            ticker(player, 100);
            double elemental = Objects.requireNonNull(player.getAttribute(AttributeInit.ELEMENTAL_DAMAGE)).getValue();
            AttackContext.run(new AttackContext(player, InteractionHand.MAIN_HAND, main, 4),
                    () -> AttackContext.phase(player, AttackContext.Phase.TRAIT,
                            () -> ((SynthesisedToolItem) main.getItem()).applyElementDamage(player, target, main)));
            close(helper, 200 - target.getHealth(), elemental, "Trait damage must not inherit motion multiplier");
            helper.succeed();
        } finally {
            BetterCombatMod.config.dual_wielding_attack_speed_multiplier = speed;
            BetterCombatMod.config.dual_wielding_main_hand_damage_multiplier = damage;
        }
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor", timeoutTicks = 80)
    public static void ordinaryMainHandCanAlternateWithSynthesisedOffHand(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack main = new ItemStack(Items.STONE_SWORD);
        ItemStack off = tool(player, ToolTypeInit.DAGGER, MaterialInit.SAMPLE);
        equip(player, main, off);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        ordinaryAttack(player, target, 0);
        helper.assertTrue(target.getHealth() < 200 && off.getDamageValue() == 0,
                "Ordinary weapon did not use its original attack path");
        helper.runAfterDelay(8, () -> {
            target.moveTo(helper.absoluteVec(new Vec3(2.5, 1, 3)));
            target.setHealth(200);
            target.invulnerableTime = 0;
            helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 1, 0)).isPresent(),
                    "Mixed offhand attack was rejected");
            helper.assertTrue(target.getHealth() < 200 && off.getDamageValue() == 1,
                    "Mixed offhand did not attack");
            helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off,
                    "Mixed inventory was not restored");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor", timeoutTicks = 80)
    public static void synthesisedMainHandCanAlternateWithOrdinaryOffHand(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack main = tool(player, ToolTypeInit.DAGGER, MaterialInit.SAMPLE);
        ItemStack off = new ItemStack(Items.STONE_SWORD);
        equip(player, main, off);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(),
                "Mixed main hand was rejected");
        helper.runAfterDelay(12, () -> {
            target.moveTo(helper.absoluteVec(new Vec3(2.5, 1, 3)));
            target.setHealth(200);
            target.invulnerableTime = 0;
            ticker(player, 100);
            ordinaryAttack(player, target, 1);
            helper.assertTrue(target.getHealth() < 200 && main.getDamageValue() == 1,
                    "Ordinary offhand was suppressed or consumed the custom weapon");
            helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off,
                    "Ordinary offhand inventory was not restored");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void generatedToolSurvivesSaveAndPayloadRoundTrip(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.SCYTHE, MaterialInit.SAMPLE);
        ItemStack restored = ItemStack.parse(player.registryAccess(), weapon.save(player.registryAccess())).orElseThrow();
        helper.assertTrue(ItemStack.isSameItemSameComponents(weapon, restored), "Tool did not survive save/reload");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                player.registryAccess(), ConnectionType.NEOFORGE);
        try {
            AttackRequest request = new AttackRequest(17, 2, 3);
            AttackRequest.CODEC.encode(buffer, request);
            helper.assertTrue(request.equals(AttackRequest.CODEC.decode(buffer)), "Attack request codec lost data");
            var effects = new AttackEffects(player.getId(),
                    true, restored, new int[]{1, 2}, Optional.of(new SlashVisual(
                    new Vec3(10, 20, 30), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1),
                    4, 2, 1, List.of(3F, 182F), 0xFFAABBCC, 0xFF112233)));
            AttackEffects.CODEC.encode(buffer, effects);
            var decoded = AttackEffects.CODEC.decode(buffer);
            helper.assertTrue(decoded.offHand() && ItemStack.isSameItemSameComponents(decoded.weapon(), restored)
                    && Arrays.equals(decoded.targets(), effects.targets())
                    && decoded.visual().equals(effects.visual()), "Visual payload lost data");
            var withoutVisual = new AttackEffects(player.getId(), false, restored, new int[0], Optional.empty());
            AttackEffects.CODEC.encode(buffer, withoutVisual);
            helper.assertTrue(AttackEffects.CODEC.decode(buffer).visual().isEmpty(),
                    "Absent visual did not survive the codec");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }


    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void unsupportedToolsKeepTheirOriginalLeftClick(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack hammer = tool(player, ToolTypeInit.HAMMER, MaterialInit.SAMPLE);
        equip(player, hammer, ItemStack.EMPTY);
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(),
                "Unsupported tool entered the custom route");
        InteractionResult result = ((SynthesisedToolItem) hammer.getItem())
                .onLeftClick(player, InteractionHand.MAIN_HAND, player.level());
        helper.assertTrue(result == InteractionResult.SUCCESS && target.getHealth() < 200,
                "Unsupported tool lost its original attack");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void breakingHitRetainsEffectsAndBrokenWeaponCannotAttack(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, ItemStack.EMPTY);
        Cow target = target(helper, 2.5F, 3);
        weapon.setDamageValue(weapon.getMaxDamage() - 2);
        ticker(player, 100);
        var result = CombatServer.attack(player, new AttackRequest(1, 0, 0));
        helper.assertTrue(result.isPresent() && target.getHealth() < 200, "Breaking hit was lost");
        ItemStack visualWeapon = result.orElseThrow().weapon();
        helper.assertTrue(ComponentHelper.isBroken(weapon) && ComponentHelper.isBroken(visualWeapon)
                && SupportedWeapons.hasParts(visualWeapon),
                "Breaking hit lost its visual weapon");
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(2, 1, 0)).isEmpty(),
                "Broken weapon was allowed to attack again");
        helper.succeed();
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void itemUseAndCombatDisabledRejectAttacks(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack weapon = tool(player, ToolTypeInit.LIGHT_SWORD, MaterialInit.SAMPLE);
        equip(player, weapon, new ItemStack(Items.SHIELD));
        Cow target = target(helper, 2.5F, 3);
        ticker(player, 100);
        player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(),
                "Attack while using an item was accepted");
        player.stopUsingItem();
        player.addTag("bettercombat_disabled");
        player.doTick();
        ticker(player, 100);
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isEmpty(),
                "Combat-disabled player was accepted");
        helper.assertTrue(target.getHealth() == 200 && weapon.getDamageValue() == 0,
                "Rejected disabled attack changed state");
        helper.succeed();
    }


    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor", timeoutTicks = 80)
    public static void comboResetsFromUpswingStartRatherThanHit(GameTestHelper helper) {
        FakePlayer player = player(helper);
        equip(player, tool(player, ToolTypeInit.DAGGER, MaterialInit.SAMPLE), ItemStack.EMPTY);
        ticker(player, 100);
        AttackHand hand = Objects.requireNonNull(PlayerAttackHelper.getCurrentAttack(player, 0));
        float cooldown = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
        int upswing = Math.max(1, Math.round(cooldown * (float) hand.upswingRate()));
        int delay = Math.round(cooldown * BetterCombatMod.config.combo_reset_rate) - upswing + 1;
        helper.assertTrue(CombatServer.attack(player, new AttackRequest(1, 0, 0)).isPresent(),
                "Initial empty swing rejected");
        helper.runAfterDelay(delay, () -> {
            helper.assertTrue(CombatServer.nextCombo(player) == 0, "Server did not reset combo at the client boundary");
            helper.assertTrue(CombatServer.attack(player, new AttackRequest(2, 0, 0)).isPresent(),
                    "Valid combo restart rejected");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void mixedDaggersCannotSelectSameCategoryStab(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack magitech = tool(player, ToolTypeInit.DAGGER, MaterialInit.SAMPLE);
        ItemStack ordinary = new ItemStack(Items.IRON_SWORD);
        ordinary.set(BetterCombatDataComponents.WEAPON_PRESET_ID,
                ResourceLocation.fromNamespaceAndPath("bettercombat", "dagger"));
        for (boolean swapped : new boolean[]{false, true}) {
            equip(player, swapped ? ordinary : magitech, swapped ? magitech : ordinary);
            helper.assertTrue(PlayerAttackHelper.isDualWielding(player), "Independent categories disabled dual wielding");
            for (int combo = 0; combo < 12; combo++) {
                var hand = Objects.requireNonNull(PlayerAttackHelper.getCurrentAttack(player, combo));
                helper.assertTrue(hand.attack().animation().contains("slash_horizontal_"),
                        "Mixed ordinary dagger selected the same-category stab");
            }
        }
        equip(player, magitech, tool(player, ToolTypeInit.DAGGER, MaterialInit.WOOD));
        for (int combo = 0; combo < 12; combo++) {
            var hand = Objects.requireNonNull(PlayerAttackHelper.getCurrentAttack(player, combo));
            helper.assertTrue(hand.attack().animation().contains("slash_horizontal_")
                            && hand.attack().damageMultiplier() == 1,
                    "Two synthesised daggers retained the special stab");
        }
        helper.succeed();
    }

    private static void ordinaryAttack(FakePlayer player, Cow target, int combo) {
        boolean vanilla = Packets.C2S_AttackRequest.UseVanillaPacket;
        try {
            // FakePlayer の vanilla 通信 handler は no-op のため、同じ server の直接実行経路を使う。
            Packets.C2S_AttackRequest.UseVanillaPacket = false;
            ServerNetwork.handleAttackRequest(new Packets.C2S_AttackRequest(combo, false, 0, target, List.of(target)),
                    player.server, player, player.connection);
        } finally {
            Packets.C2S_AttackRequest.UseVanillaPacket = vanilla;
        }
    }

    private static FakePlayer player(GameTestHelper helper) {
        // ロード確認用 template は床だけを定義しているため、生成地形を戦闘空間に残さない。
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 5; y++) {
                for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "combat-test"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1)));
        player.setYRot(0);
        player.setXRot(25);
        player.setOnGround(true);
        resetCombo(player);
        return player;
    }

    private static ItemStack tool(FakePlayer player, ToolTypeLike type, ToolMaterialLike material) {
        ItemStack stack = SynthesisedToolGenerator.generateTool(type.asToolType(), material);
        ((SynthesisedToolItem) stack.getItem()).reloadComponent(player, player.level(), stack);
        return stack;
    }

    private static void equip(FakePlayer player, ItemStack main, ItemStack off) {
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        // FakePlayer は通常の ServerPlayer.tick を実行しないため、装備属性を明示適用する。
        player.doTick();
        for (var entry : main.getAttributeModifiers().modifiers()) {
            var attribute = player.getAttribute(entry.attribute());
            if (attribute != null) attribute.addOrUpdateTransientModifier(entry.modifier());
        }
        player.setYRot(0);
        player.setXRot(25);
        player.setOnGround(true);
        ((PlayerAttackProperties) player).setComboCount(-1);
    }

    private static Cow target(GameTestHelper helper, float x, float z) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(x, 1, z));
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(200);
        cow.setHealth(200);
        return cow;
    }

    private static double damage(FakePlayer player) {
        return player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                + Objects.requireNonNull(player.getAttribute(AttributeInit.ELEMENTAL_DAMAGE)).getValue();
    }

    private static void ticker(FakePlayer player, int ticks) {
        ((AdjustableAttackStrengthEntity) player).setLastAttackedTicks(ticks);
    }

    private static void resetCombo(Player player) {
        ((PlayerAttackProperties) player).setComboCount(-1);
    }

    private static void close(GameTestHelper helper, double actual, double expected, String label) {
        helper.assertTrue(Math.abs(actual - expected) < 0.02,
                label + ": expected " + expected + ", got " + actual);
    }
}
