package jp.aquafactory.magitinkerer.datagen;

import com.google.gson.JsonObject;
import jp.aquafactory.magitinkerer.material.ToolMaterialModels;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

final class ToolMaterialModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;

    ToolMaterialModelProvider(PackOutput output) {
        models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        return CompletableFuture.allOf(ToolMaterialModels.prismarineModels().stream()
                .map(model -> saveModel(output, model))
                .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveModel(CachedOutput output, ToolMaterialModels.Model model) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", model.parent());
        json.addProperty("loader", "atlas_api:simple_model");
        json.addProperty("handler", ToolMaterialModels.HANDLER.toString());
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", model.sprite().toString());
        json.add("textures", textures);
        return DataProvider.saveStable(output, json, models.json(model.id()));
    }

    @Override
    public @NotNull String getName() {
        return "Magi Tinkerer tool material models";
    }
}
