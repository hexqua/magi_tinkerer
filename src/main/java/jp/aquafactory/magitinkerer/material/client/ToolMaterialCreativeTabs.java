package jp.aquafactory.magitinkerer.material.client;

import jp.aquafactory.magitinkerer.material.ToolMaterials;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.stln.magitech.content.item.creative_tab.CreativeTabInit;
import net.stln.magitech.helper.ComponentHelper;

public final class ToolMaterialCreativeTabs {
    private ToolMaterialCreativeTabs() {
    }

    public static void hideSoulstainedSteel(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();
        if (!tab.equals(CreativeTabInit.MAGITECH_TOOL_TAB.getKey())
                && !tab.equals(CreativeTabInit.MAGITECH_PART_TAB.getKey())) {
            return;
        }

        // Malum未導入時はSoulstained Steel系は作れずクリエ専用化し一部機能がno-opになるので一覧から除去.
        var material = ToolMaterials.SOULSTAINED_STEEL.get();
        var entries = event.getSearchEntries().stream()
                .filter(stack -> ComponentHelper.getMaterial(stack).orElse(null) == material
                        || ComponentHelper.getPartMaterials(stack).contains(material))
                .toList();
        entries.forEach(stack -> event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
