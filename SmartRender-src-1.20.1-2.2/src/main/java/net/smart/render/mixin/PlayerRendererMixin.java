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

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.render.IModelPlayer;
import net.smart.render.IRenderPlayer;
import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   Replaces net.smart.render.RenderPlayer. 1.8.9 subclassed the vanilla RenderPlayer and was
//   swapped into RenderManager by SmartRenderFactory; 1.20.1 forbids swapping the player renderer,
//   so instead we Mixin into the live PlayerRenderer and make it BE the IRenderPlayer.
//
//   Method mapping (vanilla method -> old override):
//     <init> TAIL                     -> RenderPlayer ctor   (creates the SmartRenderRender)
//     render HEAD / RETURN            -> doRender (around super.doRender)
//     setupRotations @ModifyVariable  -> rotateCorpse (around super.rotateCorpse); we feed the
//                                        vanilla body the rotation returned by setupRotationsPre
//     getBob                          -> handleRotationFloat -> see LivingEntityRendererMixin
//                                        (getBob is not overridden in PlayerRenderer)
//     renderLayers (cape/ears)        -> see CapeLayerMixin
//
//   The class extends LivingEntityRenderer only so the inherited getModel()/entityRenderDispatcher
//   and the LivingEntityRendererAccessor cast resolve at compile time; the constructor below is
//   never executed (Mixin discards it).
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
		extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
		implements IRenderPlayer {
	@Unique
	private SmartRenderRender smartrender$render;

	@Unique
	private IModelPlayer[] smartrender$allModels;

	private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model,
			float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void smartrender$initRender(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		smartrender$render = new SmartRenderRender((IRenderPlayer) this);
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void smartrender$renderPre(AbstractClientPlayer entity, float entityYaw, float partialTicks,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		boolean isInventory = SmartRenderRender.renderingInventory;
		smartrender$render.doRenderPre(entity, partialTicks, isInventory);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void smartrender$renderPost(AbstractClientPlayer entity, float entityYaw, float partialTicks,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		smartrender$render.doRenderPost();
	}

	@ModifyVariable(method = "setupRotations", at = @At("HEAD"), ordinal = 1, argsOnly = true)
	private float smartrender$setupRotations(float rotationYaw, AbstractClientPlayer entity, PoseStack poseStack,
			float ageInTicks, float partialTicks) {
		return smartrender$render.setupRotationsPre(entity, ageInTicks, rotationYaw, partialTicks);
	}

	// ---- IRenderPlayer ----

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IModelPlayer createModel(HumanoidModel<?> existing, float f, boolean b) {
		if (existing instanceof PlayerModel)
			return new net.smart.render.ModelPlayer(existing, f, b);
		return new net.smart.render.ModelBiped(existing, f);
	}

	@Override
	public void initialize(PlayerModel<?> modelBipedMain, HumanoidModel<?> modelArmorChestplate,
			HumanoidModel<?> modelArmor) {
		LivingEntityRendererAccessor self = (LivingEntityRendererAccessor) (Object) this;
		self.smartrender$setModel(modelBipedMain);

		for (RenderLayer<?, ?> layer : self.smartrender$getLayers())
			if (layer instanceof HumanoidArmorLayer) {
				HumanoidArmorLayerAccessor armor = (HumanoidArmorLayerAccessor) layer;
				armor.smartrender$setOuterModel(modelArmorChestplate);
				armor.smartrender$setInnerModel(modelArmor);
			}
	}

	@Override
	public EntityRenderDispatcher getRenderRenderManager() {
		return this.entityRenderDispatcher;
	}

	@Override
	public PlayerModel<?> getModelBipedMain() {
		return (PlayerModel<?>) this.getModel();
	}

	@Override
	public HumanoidModel<?> getModelArmorChestplate() {
		for (RenderLayer<?, ?> layer : ((LivingEntityRendererAccessor) (Object) this).smartrender$getLayers())
			if (layer instanceof HumanoidArmorLayer)
				return ((HumanoidArmorLayerAccessor) layer).smartrender$getOuterModel();
		return null;
	}

	@Override
	public HumanoidModel<?> getModelArmor() {
		for (RenderLayer<?, ?> layer : ((LivingEntityRendererAccessor) (Object) this).smartrender$getLayers())
			if (layer instanceof HumanoidArmorLayer)
				return ((HumanoidArmorLayerAccessor) layer).smartrender$getInnerModel();
		return null;
	}

	@Override
	public boolean getSmallArms() {
		return ((net.smart.render.mixin.PlayerModelAccessor) this.getModel()).smartrender$isSlim();
	}

	@Override
	public IModelPlayer[] getRenderModels() {
		if (smartrender$allModels == null)
			smartrender$allModels = new IModelPlayer[] {
					(IModelPlayer) getModelBipedMain(),
					(IModelPlayer) getModelArmorChestplate(),
					(IModelPlayer) getModelArmor()
			};
		return smartrender$allModels;
	}
}
