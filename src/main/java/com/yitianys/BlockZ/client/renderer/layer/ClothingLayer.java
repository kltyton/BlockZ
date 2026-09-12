package com.yitianys.BlockZ.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.item.BackpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ClothingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public record OuterLayerState(boolean hat, boolean jacket, boolean leftSleeve, boolean rightSleeve, boolean leftPants, boolean rightPants) { }

    public ClothingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStack stack = com.yitianys.BlockZ.equipment.EquipmentSlots.special(player, PlayerBackpack.SLOT_BACKPACK);
            if (!(stack.getItem() instanceof BackpackItem)) return;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) return;
            Minecraft minecraft = Minecraft.getInstance();
            var model = minecraft.getModelManager().getModel(new ResourceLocation(BlockZ.MODID, "item/" + id.getPath() + "_3d"));
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            poseStack.pushPose();
            try {
                this.getParentModel().body.translateAndRotate(poseStack);
                poseStack.translate(-0.5D, 1.0D, -0.5D);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(0.0D, 0.0D, 0.01D);
                if (model != null && model != minecraft.getModelManager().getMissingModel()) {
                    minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poseStack, buffer, packedLight, overlay, model);
                } else {
                    minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, packedLight, overlay, poseStack, buffer, player.level(), player.getId());
                }
            } finally {
                poseStack.popPose();
            }
        });
    }

    public static OuterLayerState captureOuterLayerState(PlayerModel<AbstractClientPlayer> model) {
        return new OuterLayerState(model.hat.visible, model.jacket.visible, model.leftSleeve.visible, model.rightSleeve.visible, model.leftPants.visible, model.rightPants.visible);
    }

    public static void restoreOuterLayerState(PlayerModel<AbstractClientPlayer> model, OuterLayerState state) {
        model.hat.visible = state.hat();
        model.jacket.visible = state.jacket();
        model.leftSleeve.visible = state.leftSleeve();
        model.rightSleeve.visible = state.rightSleeve();
        model.leftPants.visible = state.leftPants();
        model.rightPants.visible = state.rightPants();
    }
}
