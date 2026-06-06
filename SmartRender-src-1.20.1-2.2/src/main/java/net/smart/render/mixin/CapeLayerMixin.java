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
// along with Smart Render. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================

package net.smart.render.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.render.SmartRenderModel;
import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   1.8.9 RenderPlayer.renderLayers wrapped super.renderLayers with
//   render.renderSpecials(..), i.e. modelBipedMain.bipedEars.beforeRender / bipedCloak.beforeRender
//   before, and afterRender after, so Smart Render's bone transforms applied to the cape and ears.
//   In 1.20.1 the cape is drawn by the vanilla CapeLayer; this mixin wraps its render() with the
//   same before/after calls. The active main model is taken from SmartRenderRender.CurrentMainModel,
//   which PlayerRendererMixin sets for the duration of PlayerRenderer.render (the cape is drawn
//   inside that call), so the correct (default vs slim) model is always selected.
//   NOTE[ears-layer]: 1.20.1 has no standalone deadmau5-ears layer; bipedEars is wrapped here as the
//   closest faithful equivalent. Verify ear rendering at runtime.
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin
{
	@Unique
	private SmartRenderModel smartrender$active;

	@Inject(method = "render", at = @At("HEAD"))
	private void smartrender$before(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
	{
		smartrender$active = SmartRenderRender.CurrentMainModel;
		if (smartrender$active != null)
		{
			smartrender$active.bipedEars.beforeRender(player);
			smartrender$active.bipedCloak.beforeRender(player, partialTicks);
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void smartrender$after(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
	{
		if (smartrender$active != null)
		{
			smartrender$active.bipedCloak.afterRender();
			smartrender$active.bipedEars.afterRender();
			smartrender$active = null;
		}
	}
}
