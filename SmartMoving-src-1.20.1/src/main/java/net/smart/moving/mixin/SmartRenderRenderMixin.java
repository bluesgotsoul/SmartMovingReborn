// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Moving is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Moving. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================

package net.smart.moving.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.smart.moving.render.SmartMovingRender;
import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   In 1.8.9 net.smart.moving.render.RenderPlayer extended net.smart.render.RenderPlayer and
//   wrapped each render hook around its super (SmartMovingRender.doRender -> superRenderDoRender ->
//   SmartRenderRender.doRender -> ...), and it overrode createModel so Smart Render installed
//   net.smart.moving.render.ModelPlayer / ModelBiped instead of the plain Smart Render models.
//
//   1.20.1 keeps Smart Render as the single PlayerRenderer driver (net.smart.render's
//   PlayerRendererMixin builds one SmartRenderRender). Rather than racing a second PlayerRenderer
//   mixin (and to keep the exact 1.8.9 nesting without depending on injector order), Smart Moving
//   mixes into the Smart Render *render* class directly:
//     * createModel (called 3x from SmartRenderRender.<init>) is redirected to build the moving
//       models, reproducing the old createModel override;
//     * a SmartMovingRender is created at <init> TAIL, after Smart Render has installed the models;
//     * doRenderPre @HEAD / doRenderPost @RETURN reproduce the wrap order
//       SmartMoving.pre -> SmartRender.pre -> [vanilla] -> SmartRender.post -> SmartMoving.post;
//     * setupRotationsPre @HEAD runs Smart Moving's body rotation before Smart Render's, matching
//       the old rotateCorpse -> superRenderRotateCorpse order.
//   The vertical model translations (verticalRenderOffset + renderLivingAtVerticalOffset) are applied
//   by PlayerRendererMovingMixin, which reads them back through IRenderPlayer.getMovingRender().
//   NOTE[render-name]: the name-tag pass (renderNamePre / renderNamePost) is implemented by
//   LivingEntityRendererNameMixin, which wraps the EntityRenderer.render INVOKE inside
//   LivingEntityRenderer.render. shouldShowName is gated via isShiftKeyDown (flag 1).
@Mixin(SmartRenderRender.class)
public abstract class SmartRenderRenderMixin
{
	@Unique
	private SmartMovingRender smartmoving$render;

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/smart/render/IRenderPlayer;createModel(Lnet/minecraft/client/model/HumanoidModel;FZ)Lnet/smart/render/IModelPlayer;"), remap = false)
	private net.smart.render.IModelPlayer smartmoving$createMovingModel(net.smart.render.IRenderPlayer irp, HumanoidModel<?> existing, float f, boolean b)
	{
		if (existing instanceof PlayerModel)
			return new net.smart.moving.render.ModelPlayer(existing, f, b);
		return new net.smart.moving.render.ModelBiped(existing, f);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void smartmoving$initMovingRender(net.smart.render.IRenderPlayer irp, CallbackInfo ci)
	{
		smartmoving$render = new SmartMovingRender((net.smart.moving.render.IRenderPlayer)irp);
		// Hand the freshly built SmartMovingRender back to the PlayerRenderer so the render-offset mixin
		// (PlayerRendererMovingMixin) can read verticalRenderOffset / renderLivingAtVerticalOffset.
		((net.smart.moving.render.IRenderPlayer)irp).setMovingRender(smartmoving$render);
	}

	@Inject(method = "doRenderPre", at = @At("HEAD"), remap = false)
	private void smartmoving$doRenderPre(AbstractClientPlayer entityplayer, float renderPartialTicks, boolean isInventory, CallbackInfo ci)
	{
		if (smartmoving$render != null)
			smartmoving$render.doRenderPre(entityplayer, renderPartialTicks, isInventory);
	}

	@Inject(method = "doRenderPost", at = @At("RETURN"), remap = false)
	private void smartmoving$doRenderPost(CallbackInfo ci)
	{
		if (smartmoving$render != null)
			smartmoving$render.doRenderPost();
	}

	@Inject(method = "setupRotationsPre", at = @At("HEAD"), remap = false)
	private void smartmoving$setupRotationsPre(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2, CallbackInfoReturnable<Float> cir)
	{
		if (smartmoving$render != null)
			smartmoving$render.setupRotationsPre(entityplayer, totalTime, actualRotation, f2);
	}
}
