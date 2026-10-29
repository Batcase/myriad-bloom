package com.myriadbloom.client.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for Bedrock geometry files ({@code *.geo.json}).
 *
 * <p>Only what an entity renderer needs is kept: bones with their pivot, rest rotation and parent, and
 * their cubes with box UVs (or per face UVs), size and inflate. Coordinates are kept in Bedrock units
 * (16 units = 1 block, +Y up, measured from the feet) and converted when the model is baked.</p>
 */
public final class BedrockGeometry {
    /** One box of a bone. */
    public record Cube(float[] origin, float[] size, float[] uv, float inflate, Map<String, float[]> faceUv) {
    }

    /** One bone (a named group that can be animated and can parent other bones). */
    public record Bone(String name, String parent, float[] pivot, float[] rotation, List<Cube> cubes) {
    }

    private final String identifier;
    private final int textureWidth;
    private final int textureHeight;
    private final Map<String, Bone> bones;
    private final List<Bone> ordered;

    private BedrockGeometry(String identifier, int textureWidth, int textureHeight,
                            Map<String, Bone> bones, List<Bone> ordered) {
        this.identifier = identifier;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bones = bones;
        this.ordered = ordered;
    }

    public String identifier() {
        return this.identifier;
    }

    public int textureWidth() {
        return this.textureWidth;
    }

    public int textureHeight() {
        return this.textureHeight;
    }

    public Map<String, Bone> bones() {
        return this.bones;
    }

    /** Bones in file order, parents before children when the file is well formed. */
    public List<Bone> orderedBones() {
        return this.ordered;
    }

    public static BedrockGeometry parse(JsonObject json) {
        JsonObject geometry = null;
        if (json.has("minecraft:geometry")) {
            JsonElement element = json.get("minecraft:geometry");
            if (element.isJsonArray()) {
                geometry = element.getAsJsonArray().get(0).getAsJsonObject();
            } else {
                geometry = element.getAsJsonObject();
            }
        } else if (json.has("geometry")) {
            geometry = json.getAsJsonObject("geometry");
        }
        if (geometry == null) {
            throw new IllegalArgumentException("no minecraft:geometry in file");
        }
        JsonObject description = geometry.has("description")
                ? geometry.getAsJsonObject("description") : new JsonObject();
        String identifier = description.has("identifier")
                ? description.get("identifier").getAsString() : "geometry.unknown";
        int textureWidth = description.has("texture_width")
                ? description.get("texture_width").getAsInt() : 64;
        int textureHeight = description.has("texture_height")
                ? description.get("texture_height").getAsInt() : 32;

        Map<String, Bone> bones = new LinkedHashMap<>();
        List<Bone> ordered = new ArrayList<>();
        JsonArray array = geometry.has("bones") ? geometry.getAsJsonArray("bones") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
            float[] pivot = floats(bone, "pivot", new float[]{0, 0, 0});
            float[] rotation = floats(bone, "rotation", new float[]{0, 0, 0});
            List<Cube> cubes = new ArrayList<>();
            if (bone.has("cubes")) {
                for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    float[] origin = floats(cube, "origin", new float[]{0, 0, 0});
                    float[] size = floats(cube, "size", new float[]{0, 0, 0});
                    float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0.0F;
                    Map<String, float[]> faceUv = new LinkedHashMap<>();
                    float[] uv = new float[]{0, 0};
                    if (cube.has("uv")) {
                        JsonElement uvElement = cube.get("uv");
                        if (uvElement.isJsonArray()) {
                            uv = floats(cube, "uv", uv);
                        } else {
                            JsonObject map = uvElement.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                                JsonArray pair = entry.getValue().getAsJsonArray();
                                faceUv.put(entry.getKey(), new float[]{
                                        pair.get(0).getAsFloat(), pair.get(1).getAsFloat()});
                            }
                        }
                    }
                    cubes.add(new Cube(origin, size, uv, inflate, faceUv));
                }
            }
            Bone parsed = new Bone(name, parent, pivot, rotation, cubes);
            bones.put(name, parsed);
            ordered.add(parsed);
        }
        return new BedrockGeometry(identifier, textureWidth, textureHeight, bones, ordered);
    }

    private static float[] floats(JsonObject object, String key, float[] fallback) {
        if (!object.has(key)) {
            return fallback;
        }
        JsonArray array = object.getAsJsonArray(key);
        float[] values = new float[3];
        for (int i = 0; i < 3; i++) {
            values[i] = i < array.size() ? array.get(i).getAsFloat() : 0.0F;
        }
        return values;
    }
}
