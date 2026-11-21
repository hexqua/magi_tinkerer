package jp.aquafactory.magitinkerer.gametest;

import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolGenerator;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.MagicPerformanceHelper;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.tool_type.ToolTypeInit;

final class MalumSoulhunterGameTest {
    private MalumSoulhunterGameTest() {
    }

    static void verify(GameTestHelper helper) {
        var steel = ToolMaterials.SOULSTAINED_STEEL.get();
        var iron = MaterialInit.IRON.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        // 初回討伐の百科事典も Malum ではスピリットと同じ Entity として生成される。
        player.getData(MalumAttachmentTypes.PROGRESSION_DATA).obtainedEncyclopedia = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));

        var wandType = ToolTypeInit.WAND.get();
        var levelOneWand = SynthesisedToolGenerator.generateTool(wandType, steel, iron, iron, iron);
        var levelFourWand = SynthesisedToolGenerator.generateTool(wandType, steel);
        int baseDrops = harvest(helper, player, levelOneWand);
        int bonusDrops = harvest(helper, player, levelFourWand);
        helper.assertTrue(baseDrops > 0, "Soulhunter I should expose the soul on a lethal spell hit after a weapon swap");
        helper.assertTrue(bonusDrops == baseDrops + 3,
                "Soulhunter IV should drop three more spirits than Soulhunter I, got " + baseDrops + " and " + bonusDrops);
    }

    private static int harvest(GameTestHelper helper, Player player, ItemStack wand) {
        var zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        var bounds = zombie.getBoundingBox().inflate(1.0);
        var source = player.damageSources().source(Element.MAGIC.getDamageType(), player);
        MagicPerformanceHelper.applyRawMagicDamage(player, wand, zombie, source, 100.0F);
        helper.assertTrue(zombie.isDeadOrDying(), "The test spell should kill the target");

        var spirits = helper.getLevel().getEntitiesOfClass(SpiritItemEntity.class, bounds);
        int count = spirits.size();
        spirits.forEach(Entity::discard);
        zombie.discard();
        return count;
    }
}
