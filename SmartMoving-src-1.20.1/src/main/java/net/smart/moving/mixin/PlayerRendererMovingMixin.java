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

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.smart.moving.render.IModelPlayer;
import net.smart.moving.render.IRenderPlayer;
import net.smart.moving.render.SmartMovingRender;
import net.smart.render.mixin.HumanoidArmorLayerAccessor;
import net.smart.render.mixin.LivingEntityRendererAccessor;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   Replaces net.smart.moving.render.RenderPlayer. 1.8.9 subclassed net.smart.render.RenderPlayer
//   (the Smart Render player renderer) and was installed in its place; the subclass added the
//   net.smart.moving.render.IRenderPlayer accessors (getMovingRenderManager / getMovingModel*).
//   1.20.1 forbids swapping the player renderer, and Smart Render already mixes the live
//   PlayerRenderer (net.smart.render.mixin.PlayerRendererMixin) to make it the Smart Render
//   IRenderPlayer. This mixin layers the *moving* IRenderPlayer onto the very same PlayerRenderer.
//   The two mixins do not collide: every method added here is named getMoving*, and the Smart
//   Moving models that Smart Render installs (see SmartRenderRenderMixin's createModel redirect)
//   are net.smart.moving.render.ModelPlayer / ModelBiped, so the casts below resolve at runtime.
//
//   The class extends LivingEntityRenderer only so the inherited getModel() / entityRenderDispatcher
//   and the Smart Render LivingEntityRendererAccessor cast resolve at compile time; the constructor
//   below is never executed (Mixin discards it).
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMovingMixin
		extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
		implements IRenderPlayer
{
	@Unique
	private IModelPlayer[] smartmoving$allModels;

	@Unique
	private SmartMovingRender smartmoving$movingRender;

	private PlayerRendererMovingMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius)
	{
		super(context, model, shadowRadius);
	}

	@Override
	public EntityRenderDispatcher getMovingRenderManager()
	{
		return this.entityRenderDispatcher;
	}

	@Override
	public IModelPlayer getMovingModelBipedMain()
	{
		return (IModelPlayer)this.getModel();
	}

	@Override
	public IModelPlayer getMovingModelArmorChestplate()
	{
		for (RenderLayer<?, ?> layer : ((LivingEntityRendererAccessor)(Object)this).smartrender$getLayers())
			if (layer instanceof HumanoidArmorLayer)
				return (IModelPlayer)((HumanoidArmorLayerAccessor)layer).smartrender$getOuterModel();
		return null;
	}

	@Override
	public IModelPlayer getMovingModelArmor()
	{
		for (RenderLayer<?, ?> layer : ((LivingEntityRendererAccessor)(Object)this).smartrender$getLayers())
			if (layer instanceof HumanoidArmorLayer)
				return (IModelPlayer)((HumanoidArmorLayerAccessor)layer).smartrender$getInnerModel();
		return null;
	}

	@Override
	public IModelPlayer[] getMovingModels()
	{
		if (smartmoving$allModels == null)
			smartmoving$allModels = new IModelPlayer[] { getMovingModelBipedMain(), getMovingModelArmorChestplate(), getMovingModelArmor() };
		return smartmoving$allModels;
	}

	@Override
	public SmartMovingRender getMovingRender()
	{
		return smartmoving$movingRender;
	}

	@Override
	public void setMovingRender(SmartMovingRender render)
	{
		smartmoving$movingRender = render;
	}

	// 1.8.9 -> 1.20.1 PORT NOTE (vertical render offset):
	//   1.8.9 applied the Smart Moving vertical offsets inside the model-positioning GL matrix of
	//   net.smart.render.RenderPlayer.renderLivingAt: SmartMovingRender.doRender bumped the y by 0.125
	//   while crawl-sneaking (now SmartMovingRender.verticalRenderOffset) and renderLivingAt added a remote
	//   player's heightOffset (now renderLivingAtVerticalOffset). Both stacked into the one model translate,
	//   applied *before* rotateCorpse rotated the model.
	//   PlayerRenderer.render only does setModelProperties + super.render, so it carries no PoseStack push
	//   of its own; the model-positioning push and the corpse rotation both live in
	//   LivingEntityRenderer.render -> setupRotations. PlayerRenderer overrides setupRotations (Smart Render
	//   already mixes into it), so translating the PoseStack at setupRotations HEAD is the faithful
	//   equivalent: it runs inside the outer push, before the body rotations, matching the old
	//   renderLivingAt-before-rotateCorpse order. verticalRenderOffset is filled by
	//   SmartMovingRender.doRenderPre at render() HEAD (via Smart Render's doRenderPre), so it is current.
	//   NOTE[runtime-verify]: offset sign/magnitude matches 1.8.9 intent; verify in-world at build time.
	@Inject(method = "setupRotations", at = @At("HEAD"))
	private void smartmoving$applyVerticalRenderOffset(AbstractClientPlayer entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci)
	{
		SmartMovingRender render = smartmoving$movingRender;
		if (render == null)
			return;

		double offset = render.verticalRenderOffset + render.renderLivingAtVerticalOffset(entity);
		if (offset != 0.0D)
			poseStack.translate(0.0D, offset, 0.0D);
	}
}
