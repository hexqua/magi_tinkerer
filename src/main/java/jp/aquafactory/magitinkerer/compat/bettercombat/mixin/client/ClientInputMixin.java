package jp.aquafactory.magitinkerer.compat.bettercombat.mixin.client;

import jp.aquafactory.magitinkerer.compat.bettercombat.combat.SupportedWeapons;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.stln.magitech.content.event.ClientLeftClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLeftClickEvent.class, remap = false)
public abstract class ClientInputMixin {
    @Inject(method = "onLeftClick", at = @At("HEAD"), cancellable = true)
    private static void magitinkerer$skipOldInput(InputEvent.InteractionKeyMappingTriggered event,
                                                       CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && SupportedWeapons.contains(player.getMainHandItem())) ci.cancel();
    }
}
