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

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.smart.render.IModelPlayer;
import net.smart.render.ModelRotationRenderer;
import net.smart.render.SmartRenderModel;
import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
// Vanilla ItemInHandLayer.renderArmWithItem positions the held item with
// ((ArmedModel)getParentModel()).translateToHand(arm, poseStack), using the vanilla arm ModelPart.
// Smart Render never poses that vanilla part and zeroes the body yaw on the PoseStack (the whole-body
// turn lives in the bipedOuter bone), so the item froze as a "ghost" in the air at the default arm.
// We redirect that translateToHand onto the Smart Render arm bone: its parent chain
// bipedArm -> bipedShoulder -> bipedBreast -> bipedTorso -> bipedOuter already carries the body turn
// AND the animated arm pose (exactly how 1.8.9 attached items to the arm bone).
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void smartrender$translateToHand(ArmedModel model, HumanoidArm arm, PoseStack poseStack) {
        SmartRenderModel main = SmartRenderRender.CurrentMainModel;

        // Только когда этого игрока реально рисует Smart Render в мире / 3-м лице.
        // В инвентаре кости позируются чистой ванильной позой (без разворота) -> там
        // корректнее
        // штатный translateToHand, иначе предмет уехал бы вместе с инвентарной позой.
        if (main != null && !SmartRenderRender.renderingInventory && model instanceof IModelPlayer) {
            IModelPlayer mp = (IModelPlayer) model;
            ModelRotationRenderer armBone = arm == HumanoidArm.LEFT ? mp.getLeftArm() : mp.getRightArm();

            if (armBone != null) {
                PoseStack previous = ModelRotationRenderer.CurrentPoseStack;
                ModelRotationRenderer.CurrentPoseStack = poseStack;
                // postRender(f) = preTransforms(f, false, true): полная родительская цепочка +
                // поза руки
                // на ТЕКУЩУЮ матрицу без pushPose, точно как ванильный translateToHand.
                armBone.postRender(0.0625F);
                ModelRotationRenderer.CurrentPoseStack = previous;
                return;
            }
        }

        // Иначе — штатное ванильное поведение (инвентарь, не-Smart-Render сущности).
        model.translateToHand(arm, poseStack);
    }
}