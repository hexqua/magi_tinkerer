package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.resources.ResourceLocation;
import net.stln.magitech.Magitech;
import net.stln.magitech.MagitechRegistries;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.material.ToolMaterial;
import net.stln.magitech.feature.tool.model.ModelRegistrar;
import net.stln.magitech.feature.tool.part.ToolPart;
import net.stln.magitech.feature.tool.tool_type.ToolType;
import net.stln.magitech.registry.RegistryHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ToolMaterialModels {
    public static final ResourceLocation HANDLER = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "tool_materials");
    public static final ResourceLocation PALETTE_KEY = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "palettes/tool_base");

    private ToolMaterialModels() {
    }

    private static List<ToolMaterial> materials() {
        return List.of(ToolMaterials.PRISMARINE_CRYSTAL.get(), ToolMaterials.SOULSTAINED_STEEL.get(),
                ToolMaterials.MANEUVERING_COATED_GLASS.get(), ToolMaterials.RESTRAINT_COATED_GLASS.get(),
                ToolMaterials.DURUSIUM.get());
    }

    public static Map<String, ResourceLocation> palettes() {
        Map<String, ResourceLocation> palettes = new LinkedHashMap<>();
        for (ToolMaterial material : materials()) {
            palettes.put(permutation(material), material.getId().withPrefix("palettes/"));
        }
        return palettes;
    }

    public static List<Model> models() {
        return materials().stream().flatMap(material -> models(material).stream()).toList();
    }

    private static String permutation(ToolMaterial material) {
        return material.getId().getNamespace() + "_" + material.getId().getPath();
    }

    private static List<Model> models(ToolMaterial material) {
        var sample = MaterialInit.SAMPLE.get();
        String suffix = "_" + permutation(material);
        List<Model> models = new ArrayList<>();
        for (ToolPart part : RegistryHelper.registeredToolParts()) {
            if (Objects.requireNonNull(MagitechRegistries.TOOL_PART.getKey(part)).getNamespace().equals(Magitech.MOD_ID)) {
                models.add(new Model(ModelRegistrar.getPartItemModelId(material, part),
                        ModelRegistrar.getPartItemTextureId(sample, part), "minecraft:item/generated", suffix));
            }
        }
        for (ToolType tool : RegistryHelper.registeredToolTypes()) {
            if (Objects.requireNonNull(MagitechRegistries.TOOL_TYPE.getKey(tool)).getNamespace().equals(Magitech.MOD_ID)) {
                String parent = tool.heavyTool() ? "magitech:item/heavy_tool" : "minecraft:item/handheld";
                for (ToolType.PartData partData : tool.parts()) {
                    ToolPart part = partData.part().asToolPart();
                    models.add(new Model(ModelRegistrar.getPartModelId(material, tool, part),
                            ModelRegistrar.getPartTextureId(sample, tool, part), parent, suffix));
                }
            }
        }
        return models;
    }

    public record Model(ResourceLocation id, ResourceLocation source, String parent, String suffix) {
        public ResourceLocation sprite() {
            return source.withSuffix(suffix);
        }
    }
}
