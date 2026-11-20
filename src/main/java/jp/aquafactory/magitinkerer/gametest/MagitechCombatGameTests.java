package jp.aquafactory.magitinkerer.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.stln.magitech.content.entity.AdjustableAttackStrengthEntity;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;

import java.util.UUID;

@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTest がアノテーションから実行する。
public final class MagitechCombatGameTests {
    private MagitechCombatGameTests() {}

    @GameTest(templateNamespace = MagiTinkerer.MOD_ID, template = "gametest/basic_floor")
    public static void originalLeftClickAttacksWithoutBetterCombat(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 4, 4, 4)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "magitech-combat-test"));
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1)));
        player.setYRot(0);
        player.setXRot(25);
        player.setOnGround(true);

        var weapon = SynthesisedToolGenerator.generateTool(ToolTypeInit.LIGHT_SWORD.asToolType(), MaterialInit.SAMPLE);
        var item = (SynthesisedToolItem) weapon.getItem();
        item.reloadComponent(player, level, weapon);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.doTick();
        int chargeTicks = (int) Math.ceil(player.getCurrentItemAttackStrengthDelay());
        @SuppressWarnings("DataFlowIssue") // Magitech の Mixin が LivingEntity にこのインターフェースを追加する。
        var attackStrength = (AdjustableAttackStrengthEntity) player;
        attackStrength.setLastAttackedTicks(chargeTicks);

        var target = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(2.5, 1, 3));
        float health = target.getHealth();
        var result = item.onLeftClick(player, InteractionHand.MAIN_HAND, level);

        helper.assertTrue(result == InteractionResult.SUCCESS, "Magitech's original attack did not start");
        helper.assertTrue(target.getHealth() < health, "Magitech's original attack did not damage the target");
        helper.assertTrue(weapon.getDamageValue() == 1, "Magitech's original attack did not consume one durability");
        helper.succeed();
    }
}
