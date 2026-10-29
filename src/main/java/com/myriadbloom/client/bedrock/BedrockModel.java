package com.myriadbloom.client.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Bakes a {@link BedrockGeometry} into vanilla {@link ModelPart}s.
 *
 * <p>The conversion uses the same convention as hand written Java models: the ground is at {@code y = 24}
 * model units, +Y in model space points down and the renderer flips it back. A Bedrock bone pivot at
 * height {@code py} therefore sits at {@code 24 - py}, and a cube spanning {@code oy .. oy + sy} becomes a
 * box starting at {@code py - oy - sy}.</p>
 */
public final class BedrockModel {
    private final ModelPart root;
    private final Map<String, ModelPart> bones;
    private final BedrockGeometry geometry;

    private BedrockModel(ModelPart root, Map<String, ModelPart> bones, BedrockGeometry geometry) {
        this.root = root;
        this.bones = bones;
        this.geometry = geometry;
    }

    public ModelPart root() {
        return this.root;
    }

    public Map<String, ModelPart> bones() {
        return this.bones;
    }

    public BedrockGeometry geometry() {
        return this.geometry;
    }

    public static BedrockModel bake(BedrockGeometry geometry) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<String, PartDefinition> definitions = new HashMap<>();

        // Bedrock pivots are absolute model space coordinates, so every bone is attached directly to
        // the root: nesting them makes the child invisible in the baked vanilla model.
        for (BedrockGeometry.Bone bone : sorted(geometry)) {
            PartDefinition definition = root.addOrReplaceChild(bone.name(), cubes(bone), pose(bone));
            definitions.put(bone.name(), definition);
        }

        ModelPart bakedRoot = LayerDefinition.create(mesh, geometry.textureWidth(), geometry.textureHeight())
                .bakeRoot();
        Map<String, ModelPart> baked = new LinkedHashMap<>();
        for (BedrockGeometry.Bone bone : sorted(geometry)) {
            ModelPart part = bakedRoot.hasChild(bone.name()) ? bakedRoot.getChild(bone.name()) : null;
            if (part != null) {
                baked.put(bone.name(), part);
            }
        }
        return new BedrockModel(bakedRoot, baked, geometry);
    }

    /** Parents before children; bones whose parent is missing end up on the root. */
    private static List<BedrockGeometry.Bone> sorted(BedrockGeometry geometry) {
        List<BedrockGeometry.Bone> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        boolean progress = true;
        while (result.size() < geometry.orderedBones().size() && progress) {
            progress = false;
            for (BedrockGeometry.Bone bone : geometry.orderedBones()) {
                if (added.contains(bone.name())) {
                    continue;
                }
                if (bone.parent() == null || added.contains(bone.parent())) {
                    result.add(bone);
                    added.add(bone.name());
                    progress = true;
                }
            }
        }
        for (BedrockGeometry.Bone bone : geometry.orderedBones()) {
            if (!added.contains(bone.name())) {
                result.add(bone);
            }
        }
        return result;
    }

    private static PartPose pose(BedrockGeometry.Bone bone) {
        float[] pivot = bone.pivot();
        float[] rotation = bone.rotation();
        return PartPose.offsetAndRotation(pivot[0], 24.0F - pivot[1], pivot[2],
                (float) Math.toRadians(rotation[0]),
                (float) Math.toRadians(rotation[1]),
                (float) Math.toRadians(rotation[2]));
    }

    private static CubeListBuilder cubes(BedrockGeometry.Bone bone) {
        CubeListBuilder builder = CubeListBuilder.create();
        float[] pivot = bone.pivot();
        for (BedrockGeometry.Cube cube : bone.cubes()) {
            float[] origin = cube.origin();
            float[] size = cube.size();
            float x = origin[0] - pivot[0];
            float y = pivot[1] - origin[1] - size[1];
            float z = origin[2] - pivot[2];
            float[] uv = cube.uv();
            if (!cube.faceUv().isEmpty() && (uv[0] == 0.0F && uv[1] == 0.0F)) {
                // per face UVs: a single box cannot do them, so the first face offset is used for all
                uv = cube.faceUv().values().iterator().next();
            }
            builder.texOffs((int) uv[0], (int) uv[1]);
            if (cube.inflate() != 0.0F) {
                builder.addBox(x, y, z, size[0], size[1], size[2], new CubeDeformation(cube.inflate()));
            } else {
                builder.addBox(x, y, z, size[0], size[1], size[2]);
            }
        }
        return builder;
    }

    private BedrockModel() {
        throw new AssertionError();
    }
}
