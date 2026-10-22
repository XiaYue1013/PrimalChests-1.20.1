/*
 * PrimalChests by Jared (jaredlll08), LGPL-2.1.
 * Modified 2026-10-21: ported to Minecraft Forge 1.20.1 (unofficial port). RenderTileEntityPrimalChest and
 * RenderTileEntityPrimalChestAdvanced (TileEntitySpecialRenderers) are merged into this BlockEntityRenderer.
 * The transforms are the 1.12 ones. 1.0.3 never animated the lid (that code was commented out), so the
 * lid stays closed here too. Block breaking progress is drawn by the level renderer through the buffer source.
 */
package com.blamejared.primalchests.client.render;

import com.blamejared.primalchests.blockentities.PrimalChestBlockEntity;
import com.blamejared.primalchests.blocks.BlockPrimalChest;
import com.blamejared.primalchests.client.model.PrimalChestModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PrimalChestRenderer implements BlockEntityRenderer<PrimalChestBlockEntity> {

    private final ModelPart model;

    public PrimalChestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = context.bakeLayer(PrimalChestModel.LAYER);
    }

    @Override
    public void render(PrimalChestBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = Direction.SOUTH;
        if(te.hasLevel()) {
            BlockState state = te.getBlockState();
            if(state.hasProperty(BlockPrimalChest.FACING)) {
                facing = state.getValue(BlockPrimalChest.FACING);
            }
        }

        pose.pushPose();
        pose.translate(0F, 1F, 1F);
        pose.scale(1F, -1F, -1F);
        pose.translate(0.5F, 0.5F, 0.5F);
        float angle = switch(facing) {
            case NORTH -> 180F;
            case WEST -> 90F;
            case EAST -> 270F;
            default -> 0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5F, -0.5F, -0.5F);

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(te.getTier().getModelTexture()));
        model.render(pose, consumer, packedLight, packedOverlay);

        pose.popPose();
    }
}
