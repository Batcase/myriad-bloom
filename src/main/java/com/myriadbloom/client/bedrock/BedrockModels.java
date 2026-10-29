package com.myriadbloom.client.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myriadbloom.MyriadBloom;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Loads Bedrock models from {@code assets/myriadbloom/bedrock/}.
 *
 * <p>File naming: {@code <name>.geo.json} for the geometry and {@code <name>.animation.json} for the
 * animations (both optional, but the renderer only uses a model when the geometry exists). Results are
 * cached forever; a resource reload calls {@link #clear()}.</p>
 */
public final class BedrockModels {
    private static final String ROOT = "bedrock/";
    private static final Map<String, Entry> CACHE = new HashMap<>();

    private BedrockModels() {
    }

    /** A loaded model plus its animations, keyed by the animation name as written in the file. */
    public record Entry(BedrockModel model, Map<String, BedrockAnimation> animations,
                        Map<String, float[]> initialRotations) {
        public BedrockAnimation animation(String name) {
            return this.animations.get(name);
        }

        /** First animation whose name contains one of the given hints, or the single animation. */
        public BedrockAnimation animationMatching(String... hints) {
            for (String hint : hints) {
                for (Map.Entry<String, BedrockAnimation> entry : this.animations.entrySet()) {
                    if (entry.getKey().toLowerCase().contains(hint)) {
                        return entry.getValue();
                    }
                }
            }
            return this.animations.size() == 1 ? this.animations.values().iterator().next() : null;
        }
    }

    public static synchronized void clear() {
        CACHE.clear();
    }

    /** Loads (and caches) a model, returning {@code null} when the geometry file does not exist. */
    public static Entry get(String name) {
        if (CACHE.containsKey(name)) {
            return CACHE.get(name);
        }
        Entry entry = load(name);
        CACHE.put(name, entry);
        return entry;
    }

    private static Entry load(String name) {
        JsonObject geometryJson = read(name + ".geo.json");
        if (geometryJson == null) {
            MyriadBloom.LOGGER.warn("Bedrock model {} not found in assets/{}/{}", name, MyriadBloom.MODID, ROOT);
            return null;
        }
        BedrockGeometry geometry = BedrockGeometry.parse(geometryJson);
        BedrockModel model = BedrockModel.bake(geometry);
        Map<String, BedrockAnimation> animations = new LinkedHashMap<>();
        JsonObject animationJson = read(name + ".animation.json");
        if (animationJson != null) {
            animations.putAll(BedrockAnimation.parseAll(animationJson));
        }
        Map<String, float[]> initialRotations = new HashMap<>();
        model.bones().forEach((bone, part) -> initialRotations.put(bone,
                new float[]{part.xRot, part.yRot, part.zRot, part.x, part.y, part.z}));
        MyriadBloom.LOGGER.info("Bedrock model {} loaded: {} bones, {} animations", name,
                model.bones().size(), animations.size());
        return new Entry(model, animations, initialRotations);
    }

    private static JsonObject read(String relative) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, ROOT + relative);
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
            if (resource == null) {
                return null;
            }
            try (InputStream stream = resource.open();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            MyriadBloom.LOGGER.error("Failed to read Bedrock resource {}", location, exception);
            return null;
        }
    }
}
