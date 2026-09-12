package com.yitianys.BlockZ.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yitianys.BlockZ.equipment.EquipmentSlots;
import com.yitianys.BlockZ.equipment.EquipmentView;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public final class ExtraEquipmentLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidArmorModel<AbstractClientPlayer>> armor;
    private final CustomHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> head;
    private final ElytraLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> elytra;

    public ExtraEquipmentLayer(PlayerRenderer parent, EntityRendererProvider.Context context, boolean slim) {
        super(parent);
        armor = new HumanoidArmorLayer<>(parent,
                new HumanoidArmorModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager());
        head = new CustomHeadLayer<>(parent, context.getModelSet(), context.getItemInHandRenderer());
        elytra = new ElytraLayer<>(parent, context.getModelSet());
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (player.isInvisible()) return;
        for (EquipmentSlots.Entry entry : EquipmentSlots.entries(player)) {
            if (entry.managedByCurios() && CuriosRendererRegistry.getRenderer(entry.stack().getItem()).isPresent()) continue;
            if (entry.type() == EquipmentSlot.HEAD && com.yitianys.BlockZ.client.renderer.FirstPersonBodyRenderState.shouldHideHead()) continue;
            EquipmentView.query(player, entry.type(), entry.stack(), () -> {
                if (entry.stack().getItem() instanceof ArmorItem) armor.render(poses, buffer, light, player, swing, amount, partial, age, yaw, pitch);
                else if (entry.type() == EquipmentSlot.HEAD) head.render(poses, buffer, light, player, swing, amount, partial, age, yaw, pitch);
                else if (entry.type() == EquipmentSlot.CHEST) elytra.render(poses, buffer, light, player, swing, amount, partial, age, yaw, pitch);
                return null;
            });
        }
    }
}
