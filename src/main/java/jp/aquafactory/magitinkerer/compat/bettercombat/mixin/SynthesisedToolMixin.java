package jp.aquafactory.magitinkerer.compat.bettercombat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackContext;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.AttackGeometry;
import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = SynthesisedToolItem.class, remap = false)
public abstract class SynthesisedToolMixin {
    @WrapOperation(method = "sweepAttack", at = @At(value = "INVOKE",
            target = "Lnet/stln/magitech/helper/CombatHelper;getEntitiesInBox(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"))
    private List<Entity> magitinkerer$orientedTargets(Level world, Entity owner, Vec3 center,
                                                           Vec3 size, Operation<List<Entity>> original) {
        if (!world.isClientSide && owner instanceof Player player) {
            AttackContext context = AttackContext.current(player);
            if (context != null) {
                var box = AttackGeometry.resolve(world, player, player.entityInteractionRange(), size.x);
                // 特性が追加攻撃を行っても、武器本体の表示位置を置き換えない。
                if (context.geometry == null) context.geometry = box.orElse(null);
                return box.map(value -> AttackGeometry.findTargets(world, player, value)).orElseGet(List::of);
            }
        }
        return original.call(world, owner, center, size);
    }

    @Inject(method = "onLeftClick", at = @At("HEAD"), cancellable = true)
    private void magitinkerer$disableImmediateAttack(Player user, InteractionHand hand, Level world,
                                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (SupportedWeapons.contains((Item) (Object) this)) cir.setReturnValue(InteractionResult.PASS);
    }

    @WrapOperation(method = "sweepAttack", at = @At(value = "INVOKE",
            target = "Lnet/stln/magitech/content/item/tool/toolitem/SynthesisedToolItem;applyElementDamage(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void magitinkerer$elementPhase(SynthesisedToolItem item, Player attacker, Entity target,
                                                 ItemStack stack, Operation<Void> original) {
        AttackContext.phase(attacker, AttackContext.Phase.ELEMENTAL,
                () -> original.call(item, attacker, target, stack));
    }

    @WrapOperation(method = "sweepAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void magitinkerer$physicalPhase(Player player, Entity target, Operation<Void> original) {
        AttackContext.phase(player, AttackContext.Phase.PHYSICAL, () -> original.call(player, target));
    }

    @WrapOperation(method = "sweepAttack", at = @At(value = "INVOKE",
            target = "Lnet/stln/magitech/content/item/tool/toolitem/SynthesisedToolItem;callTraitDamageEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void magitinkerer$traitPhase(SynthesisedToolItem item, Level world, Player user,
                                              Entity target, ItemStack stack, Operation<Void> original) {
        AttackContext.record(user, target);
        AttackContext.phase(user, AttackContext.Phase.TRAIT, () -> original.call(item, world, user, target, stack));
    }

    @WrapOperation(method = "applyElementDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D"))
    private double magitinkerer$elementDamage(AttributeInstance attribute, Operation<Double> original,
                                                    Player attacker, Entity target, ItemStack stack) {
        return AttackContext.scale(attacker, AttackContext.Phase.ELEMENTAL, original.call(attribute));
    }

    @WrapOperation(method = "damage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void magitinkerer$damageSlot(ItemStack stack, int amount, LivingEntity entity,
                                              EquipmentSlot slot, Operation<Void> original) {
        AttackContext context = entity instanceof Player player ? AttackContext.current(player) : null;
        EquipmentSlot usedSlot = context != null && context.weapon == stack
                && context.hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : slot;
        original.call(stack, amount, entity, usedSlot);
    }
}
