package jp.aquafactory.magitinkerer.material;

import jp.aquafactory.magitinkerer.MagiTinkerer;
import net.minecraft.resources.ResourceLocation;
import net.stln.magitech.Magitech;
import net.stln.magitech.MagitechRegistries;
import net.stln.magitech.feature.tool.material.MaterialInit;
import net.stln.magitech.feature.tool.model.ModelRegistrar;
import net.stln.magitech.feature.tool.part.ToolPart;
import net.stln.magitech.feature.tool.tool_type.ToolType;
import net.stln.magitech.registry.RegistryHelper;

import java.util.ArrayList;
import java.util.List;

public final class ToolMaterialModels {
    public static final ResourceLocation HANDLER = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "tool_materials");
    public static final ResourceLocation PALETTE_KEY = ResourceLocation.fromNamespaceAndPath(MagiTinkerer.MOD_ID, "palettes/tool_base");
    public static final String PRISMARINE_PERMUTATION = MagiTinkerer.MOD_ID + "_" + ToolMaterials.PRISMARINE_CRYSTAL.getId().getPath();
    public static final ResourceLocation PRISMARINE_PALETTE = ToolMaterials.PRISMARINE_CRYSTAL.getId().withPrefix("palettes/");

    private ToolMaterialModels() {
    }

    public static List<Model> prismarineModels() {
        var sample = MaterialInit.SAMPLE.get();
        var prismarine = ToolMaterials.PRISMARINE_CRYSTAL.get();
        List<Model> models = new ArrayList<>();
        for (ToolPart part : RegistryHelper.registeredToolParts()) {
            if (MagitechRegistries.TOOL_PART.getKey(part).getNamespace().equals(Magitech.MOD_ID)) {
                models.add(new Model(ModelRegistrar.getPartItemModelId(prismarine, part),
                        ModelRegistrar.getPartItemTextureId(sample, part), "minecraft:item/generated"));
            }
        }
        for (ToolType tool : RegistryHelper.registeredToolTypes()) {
            if (MagitechRegistries.TOOL_TYPE.getKey(tool).getNamespace().equals(Magitech.MOD_ID)) {
                String parent = tool.heavyTool() ? "magitech:item/heavy_tool" : "minecraft:item/handheld";
                for (ToolType.PartData partData : tool.parts()) {
                    ToolPart part = partData.part().asToolPart();
                    models.add(new Model(ModelRegistrar.getPartModelId(prismarine, tool, part),
                            ModelRegistrar.getPartTextureId(sample, tool, part), parent));
                }
            }
        }
        return models;
    }

    public record Model(ResourceLocation id, ResourceLocation source, String parent) {
        public ResourceLocation sprite() {
            return source.withSuffix("_" + PRISMARINE_PERMUTATION);
        }
    }
}
