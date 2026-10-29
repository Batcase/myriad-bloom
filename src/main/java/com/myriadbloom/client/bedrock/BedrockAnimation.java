package com.myriadbloom.client.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;

/**
 * Parser and sampler for Bedrock animation files ({@code *.animation.json}).
 *
 * <p>Supports the common subset: per bone {@code rotation} / {@code position} / {@code scale} channels made
 * of {@code [time, value]} pairs, linear interpolation, looping or one shot playback, and a small Molang
 * subset for expressions such as {@code "math.sin(query.anim_time * 90) * 2"}.</p>
 */
public final class BedrockAnimation {
    /** A sampled pose of one bone. */
    public record Pose(float[] rotation, float[] position, float[] scale) {
        static final Pose IDENTITY = new Pose(new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{1, 1, 1});
    }

    /** A keyframe channel: times in seconds plus the values (each 3 wide). */
    private record Channel(float[] times, float[][] values) {
    }

    private final String name;
    private final float length;
    private final boolean loop;
    private final Map<String, Channel> rotations = new LinkedHashMap<>();
    private final Map<String, Channel> positions = new LinkedHashMap<>();
    private final Map<String, Channel> scales = new LinkedHashMap<>();

    private BedrockAnimation(String name, float length, boolean loop) {
        this.name = name;
        this.length = length;
        this.loop = loop;
    }

    public String name() {
        return this.name;
    }

    public float length() {
        return this.length;
    }

    public boolean loop() {
        return this.loop;
    }

    /** All animations of a file, keyed by name (the {@code animation.} prefix is kept as written). */
    public static Map<String, BedrockAnimation> parseAll(JsonObject json) {
        Map<String, BedrockAnimation> result = new LinkedHashMap<>();
        JsonObject animations = json.has("animations") ? json.getAsJsonObject("animations") : json;
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject animation = entry.getValue().getAsJsonObject();
            float length = animation.has("animation_length")
                    ? animation.get("animation_length").getAsFloat() : 0.0F;
            boolean loop = animation.has("loop") && animation.get("loop").getAsBoolean();
            BedrockAnimation parsed = new BedrockAnimation(entry.getKey(), length, loop);
            if (animation.has("bones")) {
                JsonObject bones = animation.getAsJsonObject("bones");
                for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                    if (!boneEntry.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject bone = boneEntry.getValue().getAsJsonObject();
                    Channel rotation = channel(bone.get("rotation"));
                    Channel position = channel(bone.get("position"));
                    Channel scale = channel(bone.get("scale"));
                    if (rotation != null) {
                        parsed.rotations.put(boneEntry.getKey(), rotation);
                    }
                    if (position != null) {
                        parsed.positions.put(boneEntry.getKey(), position);
                    }
                    if (scale != null) {
                        parsed.scales.put(boneEntry.getKey(), scale);
                    }
                }
            }
            result.put(entry.getKey(), parsed);
        }
        return result;
    }

    /** Reads one channel, either a constant array or a list of {@code {time, value}} keyframes. */
    private static Channel channel(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            float[] value = vec(element.getAsJsonArray());
            return new Channel(new float[]{0.0F}, new float[][]{value});
        }
        if (!element.isJsonObject()) {
            // a string expression such as "math.sin(query.anim_time * 90)" is not supported: treat as zero
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) {
            return channel(object.get("vector"));
        }
        List<Float> times = new ArrayList<>();
        List<float[]> values = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            float time;
            try {
                time = Float.parseFloat(entry.getKey());
            } catch (NumberFormatException ignored) {
                continue;
            }
            JsonElement valueElement = entry.getValue();
            if (valueElement.isJsonObject() && valueElement.getAsJsonObject().has("post")) {
                valueElement = valueElement.getAsJsonObject().get("post");
            }
            if (valueElement.isJsonArray()) {
                times.add(time);
                values.add(vec(valueElement.getAsJsonArray()));
            }
        }
        if (times.isEmpty()) {
            return null;
        }
        // the file order of JSON objects is preserved by Gson, but sort by time to be safe
        Integer[] order = new Integer[times.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Float.compare(times.get(a), times.get(b)));
        float[] sortedTimes = new float[order.length];
        float[][] sortedValues = new float[order.length][];
        for (int i = 0; i < order.length; i++) {
            sortedTimes[i] = times.get(order[i]);
            sortedValues[i] = values.get(order[i]);
        }
        return new Channel(sortedTimes, sortedValues);
    }

    private static float[] vec(JsonArray array) {
        float[] values = new float[3];
        for (int i = 0; i < 3; i++) {
            if (i < array.size()) {
                JsonElement element = array.get(i);
                values[i] = element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                        ? element.getAsFloat() : Molang.eval(element.getAsString(), 0.0F);
            } else {
                values[i] = i == 2 ? 1.0F : 0.0F;
            }
        }
        return values;
    }

    /** Samples the animation at {@code seconds}, returning the pose of one bone. */
    public Pose sample(String bone, float seconds) {
        float time = seconds;
        if (this.length > 0.0F) {
            time = this.loop ? time % this.length : Math.min(time, this.length);
        }
        float[] rotation = sampleChannel(this.rotations.get(bone), time, new float[]{0, 0, 0});
        float[] position = sampleChannel(this.positions.get(bone), time, new float[]{0, 0, 0});
        float[] scale = sampleChannel(this.scales.get(bone), time, new float[]{1, 1, 1});
        return new Pose(rotation, position, scale);
    }

    public boolean hasBone(String bone) {
        return this.rotations.containsKey(bone) || this.positions.containsKey(bone)
                || this.scales.containsKey(bone);
    }

    private static float[] sampleChannel(Channel channel, float time, float[] fallback) {
        if (channel == null || channel.times().length == 0) {
            return fallback;
        }
        if (channel.times().length == 1) {
            return channel.values()[0];
        }
        float[] times = channel.times();
        if (time <= times[0]) {
            return channel.values()[0];
        }
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                float span = times[i] - times[i - 1];
                float t = span <= 0.0F ? 0.0F : (time - times[i - 1]) / span;
                t = Mth.clamp(t, 0.0F, 1.0F);
                float[] a = channel.values()[i - 1];
                float[] b = channel.values()[i];
                return new float[]{
                        Mth.lerp(t, a[0], b[0]),
                        Mth.lerp(t, a[1], b[1]),
                        Mth.lerp(t, a[2], b[2])};
            }
        }
        return channel.values()[times.length - 1];
    }

    /** The animation length, or the last keyframe time when the file omits it. */
    public float effectiveLength() {
        if (this.length > 0.0F) {
            return this.length;
        }
        float max = 0.0F;
        for (Channel channel : concat()) {
            if (channel != null && channel.times().length > 0) {
                max = Math.max(max, channel.times()[channel.times().length - 1]);
            }
        }
        return max;
    }

    private List<Channel> concat() {
        List<Channel> all = new ArrayList<>(this.rotations.values());
        all.addAll(this.positions.values());
        all.addAll(this.scales.values());
        return all;
    }

    /** Tiny Molang subset used for numbers inside arrays. */
    static final class Molang {
        private final String text;
        private int index;
        private final float animTime;

        private Molang(String text, float animTime) {
            this.text = text;
            this.animTime = animTime;
        }

        static float eval(String expression, float animTime) {
            try {
                Molang parser = new Molang(expression, animTime);
                float value = parser.expression();
                return Float.isFinite(value) ? value : 0.0F;
            } catch (RuntimeException ignored) {
                return 0.0F;
            }
        }

        private float expression() {
            float value = term();
            while (true) {
                skipSpace();
                if (peek('+')) {
                    this.index++;
                    value += term();
                } else if (peek('-')) {
                    this.index++;
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        private float term() {
            float value = factor();
            while (true) {
                skipSpace();
                if (peek('*')) {
                    this.index++;
                    value *= factor();
                } else if (peek('/')) {
                    this.index++;
                    float divisor = factor();
                    value = divisor == 0.0F ? 0.0F : value / divisor;
                } else {
                    return value;
                }
            }
        }

        private float factor() {
            skipSpace();
            if (peek('(')) {
                this.index++;
                float value = expression();
                skipSpace();
                if (peek(')')) {
                    this.index++;
                }
                return value;
            }
            if (peek('-')) {
                this.index++;
                return -factor();
            }
            if (this.index < this.text.length() && Character.isDigit(this.text.charAt(this.index))) {
                int start = this.index;
                while (this.index < this.text.length()
                        && (Character.isDigit(this.text.charAt(this.index)) || this.text.charAt(this.index) == '.')) {
                    this.index++;
                }
                return Float.parseFloat(this.text.substring(start, this.index));
            }
            String name = identifier();
            return switch (name) {
                case "query.anim_time", "q.anim_time" -> this.animTime;
                case "query.life_time", "q.life_time" -> this.animTime;
                case "math.pi" -> (float) Math.PI;
                case "math.sin" -> (float) Math.sin(Math.toRadians(argument()));
                case "math.cos" -> (float) Math.cos(Math.toRadians(argument()));
                case "math.abs" -> Math.abs(argument());
                default -> 0.0F;
            };
        }

        private float argument() {
            skipSpace();
            if (!peek('(')) {
                return 0.0F;
            }
            this.index++;
            float value = expression();
            skipSpace();
            if (peek(')')) {
                this.index++;
            }
            return value;
        }

        private String identifier() {
            int start = this.index;
            while (this.index < this.text.length()
                    && (Character.isLetterOrDigit(this.text.charAt(this.index))
                    || this.text.charAt(this.index) == '.' || this.text.charAt(this.index) == '_')) {
                this.index++;
            }
            return this.text.substring(start, this.index);
        }

        private boolean peek(char c) {
            skipSpace();
            return this.index < this.text.length() && this.text.charAt(this.index) == c;
        }

        private void skipSpace() {
            while (this.index < this.text.length() && Character.isWhitespace(this.text.charAt(this.index))) {
                this.index++;
            }
        }
    }

    private BedrockAnimation() {
        throw new AssertionError();
    }

    /** Unused marker so javac keeps the identity constant referenced. */
    static Pose identity() {
        return Pose.IDENTITY;
    }
}
