// ==================================================================
// This file is part of Smart Render.
//
// Smart Render is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Render is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Render. If not, see.
// ==================================================================

package net.smart.render.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.render.ModelCapeRenderer;
import net.smart.render.ModelRotationRenderer;
import net.smart.render.SmartRenderModel;
import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
// Vanilla CapeLayer draws the cape from the player's logical yBodyRot, but Smart Render zeroes the body
// yaw on the PoseStack (the visible turn lives in the bipedOuter bone). So the cape ignored the visible
// 180-degree turn / body lean. The cape geometry is already captured into the bipedCloak bone (copy ->
// setGeometry) and ModelCapeRenderer holds the full ported cape physics. We cancel the vanilla draw and
// render the cape through the bone: its chain bipedCloak -> bipedBreast -> bipedTorso -> bipedOuter
// carries the body turn/lean, and ModelCapeRenderer.preTransform adds the cape swing.
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void smartrender$renderCape(
			PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
			float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
			CallbackInfo ci) {
		SmartRenderModel main = SmartRenderRender.CurrentMainModel;
		ModelCapeRenderer cloak = main == null ? null : main.bipedCloak;
		if (main == null || cloak == null)
			return; // не Smart Render — пусть рисует ваниль

		// Те же условия видимости кейпа, что и в ванильном CapeLayer 1.20.1.
		if (!player.isCapeLoaded()
				|| player.isInvisible()
				|| !player.isModelPartShown(PlayerModelPart.CAPE)
				|| player.getCloakTextureLocation() == null) {
			ci.cancel();
			return; // кейп не виден — ничего не рисуем (и не даём ванили рисовать в неверном кадре)
		}

		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chest.is(Items.ELYTRA))
			return; // элитры рисует собственный слой — оставляем ванильное поведение

		// Берём отрисовку кейпа на себя — рисуем костью bipedCloak.
		ci.cancel();

		VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(player.getCloakTextureLocation()));

		PoseStack prevPose = ModelRotationRenderer.CurrentPoseStack;
		VertexConsumer prevBuffer = ModelRotationRenderer.CurrentBuffer;
		int prevLight = ModelRotationRenderer.CurrentLight;
		int prevOverlay = ModelRotationRenderer.CurrentOverlay;

		ModelRotationRenderer.CurrentPoseStack = poseStack;
		ModelRotationRenderer.CurrentBuffer = consumer;
		ModelRotationRenderer.CurrentLight = packedLight;
		ModelRotationRenderer.CurrentOverlay = OverlayTexture.NO_OVERLAY;

		// beforeRender(player, partialTicks): запоминает игрока/partialTicks для физики
		// кейпа и зовёт
		// super.beforeRender(true) -> doPopPush=true, ignoreRender=false. При render()
		// механизм doPopPush
		// снимает наш pushPose (сброс к базе сущности), затем применяется полная
		// цепочка костей
		// (разворот/наклон тела) + ModelCapeRenderer.preTransform (колыхание).
		poseStack.pushPose();
		cloak.beforeRender(player, partialTicks);
		cloak.render(0.0625F);
		cloak.afterRender();
		poseStack.popPose();

		ModelRotationRenderer.CurrentPoseStack = prevPose;
		ModelRotationRenderer.CurrentBuffer = prevBuffer;
		ModelRotationRenderer.CurrentLight = prevLight;
		ModelRotationRenderer.CurrentOverlay = prevOverlay;
	}
}