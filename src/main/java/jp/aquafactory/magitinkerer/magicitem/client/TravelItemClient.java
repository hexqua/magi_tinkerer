package jp.aquafactory.magitinkerer.magicitem.client;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import jp.aquafactory.magitinkerer.magicitem.TravelEffects;
import jp.aquafactory.magitinkerer.magicitem.TravelItem;
import jp.aquafactory.magitinkerer.magicitem.TravelItemContent;
import jp.aquafactory.magitinkerer.machine.MenderContent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.stln.magitech.effect.visual.Section;
import net.stln.magitech.effect.visual.preset.AreaVFX;
import net.stln.magitech.effect.visual.preset.EntityVFX;
import net.stln.magitech.effect.visual.preset.PointVFX;
import net.stln.magitech.effect.visual.spawner.ElementParticles;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.magic.cooldown.CooldownHelper;
import net.stln.magitech.helper.ComponentHelper;
import net.stln.magitech.helper.EnergyFormatter;
import net.stln.magitech.helper.ManaContainerHelper;
import net.stln.magitech.helper.MathHelper;
import net.stln.magitech.helper.RenderHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TravelItemClient {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static int chargeTicks;
    private static int awaitUseTicks;

    private TravelItemClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(TravelItemClient::registerTooltip);
        bus.addListener(TravelItemClient::registerHud);
        NeoForge.EVENT_BUS.addListener(TravelItemClient::tick);
    }

    private static void registerTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TravelItem.ManaTooltip.class, ManaGauge::new);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "travel_charge"),
                TravelItemClient::renderHud);
    }

    public static void describe(TravelItem item, ItemStack stack, List<Component> lines) {
        var spell = item.spell();
        var config = spell.getConfig();
        int spellColor = config.element().getTextColor().getRGB();
        var spellName = spell.getName().withColor(spellColor);
        var player = Minecraft.getInstance().player;
        String key = stack.getDescriptionId();
        ManaContainerHelper.addManaContainerItemInfo(ComponentHelper.getMana(stack),
                ComponentHelper.getMaxMana(stack), ComponentHelper.getMaxFlow(stack), lines, 0xcdffde, 0x5a9e91);
        lines.add(hint(key + ".desc_1", spellName));
        var menderName = MenderContent.MENDER.get().getName().withColor(Element.MANA.getTextColor().getRGB());
        if (item == TravelItemContent.DIMENSIONAL_MIRROR.get()) {
            lines.add(hint(key + ".desc_2", menderName));
        } else {
            lines.add(hint(key + ".desc_2"));
            lines.add(item.isMirror() ? hint(key + ".desc_3", menderName) : hint(key + ".desc_3"));
        }
        lines.add(Component.empty());
        lines.add(spellName);
        if (player != null) {
            lines.addAll(spell.getDescription(player.level(), player, stack));
            for (Component line : spell.getTooltip(player.level(), player, TravelItem.heldWand(player))) {
                var color = line.getStyle().getColor();
                int dimmed = ((color == null ? 0x808080 : color.getValue()) & 0xfefefe) >>> 1;
                lines.add(line.copy().withColor(dimmed));
            }
            if (CooldownHelper.isCooldown(player, spell)) {
                lines.add(Component.translatable("item.magitinkerer.common.magic_item.cool_down",
                        seconds(CooldownHelper.getCooldownTime(player, spell))).withColor(0xff8080));
            }
        }
    }

    private static Component hint(String key, Component... arguments) {
        return Component.translatable(key, (Object[]) arguments).withColor(0xc0c0c0);
    }

    public static int barColor(TravelItem item, ItemStack stack, int manaColor) {
        return ComponentHelper.getMana(stack) < item.manaCost(Minecraft.getInstance().player) ? 0xff5555 : manaColor;
    }

    public static void updateCharge(TravelEffects.Charge payload) {
        chargeTicks = payload.ticks();
        awaitUseTicks = chargeTicks > 0 ? 5 : 0;
    }

    private static void renderHud(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (chargeTicks <= 0 || player == null || minecraft.options.hideGui || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof TravelItem item)) {
            return;
        }
        Element element = item.spell().getConfig().element();
        var texture = ResourceLocation.fromNamespaceAndPath("magitech", "textures/gui/spell/" + element.get() + "_charge_gauge.png");
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() * 2 / 3;
        int elapsed = player.getTicksUsingItem();
        int width = (int) (40 * Math.clamp((elapsed + delta.getGameTimeDeltaPartialTick(true)) / chargeTicks, 0, 1));
        graphics.blit(texture, x - 24, y - 12, 0, 0, 48, 23, 88, 24);
        if (width > 0) {
            graphics.blit(texture, x - 20, y - 6, 48, 0, width, 11, 88, 24);
        }
        String time = seconds(Math.max(0, chargeTicks - elapsed));
        RenderHelper.renderFramedText(graphics, minecraft.font, time, x - minecraft.font.width(time) / 2, y - 4, element);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isUsingItem()
                && minecraft.player.getUseItem().getItem() instanceof TravelItem) {
            awaitUseTicks = 0;
        } else if (minecraft.player == null || awaitUseTicks-- <= 0) {
            chargeTicks = 0;
            awaitUseTicks = 0;
        }
        var level = minecraft.level;
        if (level == null || minecraft.isPaused() || level.getGameTime() % 3 != 0) {
            return;
        }
        for (var player : level.players()) {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof TravelItem item) {
                var element = item.spell().getConfig().element();
                PointVFX.ringSquare(level, player.position(), element, UP, 1, 0.15F, 0.5F, 0);
                EntityVFX.powerupAura(level, element, player, Section.cover(), 1);
            }
        }
    }

    public static void flash(TravelEffects.Flash payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) {
            return;
        }
        Element element = payload.mirror() ? Element.HOLLOW : Element.PHANTOM;
        PointVFX.ringSquare(level, payload.pos(), element, UP, 30, 0.2F, 1.2F, 0.05F);
        PointVFX.ring(level, payload.pos(), element,
                payload.mirror() ? ElementParticles::riftParticle : ElementParticles::glintParticle,
                UP, 30, 0.2F, 1.2F, 0.05F);
        AreaVFX.areaLight(level, element, payload.pos(), 1.0F, 10.0F, 40);
    }

    private static String seconds(int ticks) {
        return MathHelper.round(ticks / 20.0, 1) + "s";
    }

    private record ManaGauge(TravelItem.ManaTooltip data) implements ClientTooltipComponent {
        private static final ResourceLocation MANA_SPRITE = ResourceLocation.fromNamespaceAndPath("magitech", "mana");
        private static final int BAR_HEIGHT = 14;
        private static final int BOTTOM_PADDING = 5;

        @Override
        public int getHeight() {
            return BAR_HEIGHT + BOTTOM_PADDING;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return 120;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics graphics) {
            graphics.fill(x, y, x + 120, y + BAR_HEIGHT, 0x60ffffff);
            graphics.fill(x + 1, y + 1, x + 119, y + 13, 0xff303030);
            int width = (int) Math.round(118 * Math.clamp((double) data.mana() / data.capacity(), 0, 1));
            for (int offset = 0; offset < width; offset += 16) {
                graphics.blitSprite(MANA_SPRITE, 16, 16, 0, 2, x + 1 + offset, y + 1,
                        Math.min(16, width - offset), 12);
            }
            graphics.drawString(font, EnergyFormatter.formatEnergy(data.mana(), data.capacity()), x + 4, y + 4, 0xffffffff);
        }
    }
}
