/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). New file: 1.20 no longer has the
 * 1.12 ModelChest the chests were drawn with, and the vanilla 1.20 chest model expects a re-laid-out texture.
 */
package com.blamejared.primalchests.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.blamejared.primalchests.reference.Reference.MOD_ID;

/**
 * The geometry of the 1.12 {@code ModelChest} (boxes, pivots and texture offsets), so the original 64x64 textures
 * map exactly as before.
 */
@OnlyIn(Dist.CLIENT)
public final class PrimalChestModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(MOD_ID, "primal_chest"), "main");

    public static final String LID = "lid";
    public static final String KNOB = "knob";
    public static final String BOTTOM = "bottom";

    private PrimalChestModel() {}

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14, 5, 14), PartPose.offset(1.0F, 7.0F, 15.0F));
        root.addOrReplaceChild(KNOB, CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2, 4, 1), PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 0.0F, 14, 10, 14), PartPose.offset(1.0F, 6.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
