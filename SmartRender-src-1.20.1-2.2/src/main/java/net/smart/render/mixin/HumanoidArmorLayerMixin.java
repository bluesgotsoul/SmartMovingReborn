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

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.render.IModelPlayer;

// 1.8.9 -> 1.20.1 PORT NOTE:
// Vanilla HumanoidArmorLayer poses the armor model ONLY via parentModel.copyPropertiesTo(armorModel);
// it never calls setupAnim on the armor model. Smart Render swaps in bone-driven armor models
// (PlayerRendererMixin.initialize) whose pose lives in an internal ModelRotationRenderer bone tree
// that copyPropertiesTo does not touch -> the armor stayed at its reset pose (empty mannequin facing
// south). This hook runs the SAME setupAnim the body already uses on the swapped-in armor models, so
// their bones are posed identically (including the bipedOuter whole-body rotation that
// SmartRenderRender.doRenderPre/setupRotationsPre push onto EVERY model from getRenderModels()).
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "render", at = @At("HEAD"))
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void smartrender$animateArmor(
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            LivingEntity entity, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        HumanoidArmorLayerAccessor self = (HumanoidArmorLayerAccessor) (Object) this;

        HumanoidModel inner = self.smartrender$getInnerModel();
        if (inner instanceof IModelPlayer)
            inner.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        HumanoidModel outer = self.smartrender$getOuterModel();
        if (outer instanceof IModelPlayer)
            outer.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}