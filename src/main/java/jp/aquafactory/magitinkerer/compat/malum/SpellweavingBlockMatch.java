package jp.aquafactory.magitinkerer.compat.malum;

import com.sammy.malum.common.item.curiosities.tools.spellweaver.SpellweavingPickaxeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SpellweavingBlockMatch {
    private static List<Set<Block>> previewGroups = List.of();

    private SpellweavingBlockMatch() {}

    public static boolean matches(Level level, BlockState origin, BlockState candidate) {
        Block originBlock = origin.getBlock();
        Block candidateBlock = candidate.getBlock();
        if (originBlock == candidateBlock) {
            return true;
        }
        if (!level.isClientSide()) {
            return SpellweavingPickaxeItem.matches(origin, candidate);
        }
        if (candidate.isEmpty()) {
            return false;
        }
        // 統合サーバーと同じ JVM でも、プレビューは接続先から同期されたグループを使う。
        for (var group : previewGroups) {
            if (group.contains(originBlock) && group.contains(candidateBlock)) {
                return true;
            }
        }
        return false;
    }

    public static void updatePreviewGroups(List<List<ResourceLocation>> groups) {
        previewGroups = groups.stream()
                .map(group -> group.stream()
                        .map(BuiltInRegistries.BLOCK::getOptional)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toUnmodifiableSet()))
                .toList();
    }

    public static void clearPreviewGroups() {
        previewGroups = List.of();
    }
}
