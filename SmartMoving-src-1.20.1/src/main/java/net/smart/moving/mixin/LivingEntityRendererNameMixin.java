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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.moving.render.IRenderPlayer;
import net.smart.moving.render.SmartMovingRender;

// 1.8.9 -> 1.20.1 PORT NOTE (name tag):
//   1.8.9 net.smart.moving.render.RenderPlayer.renderName wrapped super.renderName: it briefly flipped
//   the player's isSneaking flag (so the vanilla name-visibility logic followed Config._crawlNameTag /
//   Config._sneakNameTag) and nudged the name's vertical position (d1 -= 0.2 / 0.05), then restored
//   sneaking. In 1.20.1 the name pass is in neither PlayerRenderer.render nor LivingEntityRenderer.render
//   directly: LivingEntityRenderer.render finishes the body, pops its pose, then calls super.render
//   (EntityRenderer.render), which is the { if (shouldShowName) renderNameTag } block. So the faithful
//   wrap is around that super.render INVOKE inside LivingEntityRenderer.render:
//     * BEFORE -> renderNamePre flips the shift-key state (feeding shouldShowName's isDiscrete() check,
//       exactly like the old setSneaking trick) and returns the d1 nudge, applied as a scoped PoseStack
//       push + translate so only the name tag moves;
//     * AFTER  -> pop the pose and renderNamePost restores the original shift-key state.
//   Because the body is already drawn (and its pose popped) before super.render, flipping the shift flag
//   here changes only the name pass, never the body pose -- matching the 1.8.9 scoping.
//   Guarded by `this instanceof IRenderPlayer` (only the Smart Moving PlayerRenderer) + AbstractClientPlayer,
//   so every other living renderer is left untouched.
//   NOTE[runtime-verify]: name visibility + vertical offset match 1.8.9 intent; verify in-world at build time.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameMixin
{
	@Unique
	private boolean smartmoving$nameWrapped;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.BEFORE))
	private void smartmoving$renderNamePre(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci)
	{
		smartmoving$nameWrapped = false;
		if (!((Object)this instanceof IRenderPlayer) || !(entity instanceof AbstractClientPlayer))
			return;

		SmartMovingRender render = ((IRenderPlayer)this).getMovingRender();
		if (render == null)
			return;

		double d1Delta = render.renderNamePre((AbstractClientPlayer)entity);
		poseStack.pushPose();
		poseStack.translate(0.0D, d1Delta, 0.0D);
		smartmoving$nameWrapped = true;
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER))
	private void smartmoving$renderNamePost(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci)
	{
		if (!smartmoving$nameWrapped)
			return;

		smartmoving$nameWrapped = false;
		poseStack.popPose();
		((IRenderPlayer)this).getMovingRender().renderNamePost((AbstractClientPlayer)entity);
	}
}
