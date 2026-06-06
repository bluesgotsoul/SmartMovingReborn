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

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.smart.render.statistics.SmartStatistics;
import net.smart.render.statistics.SmartStatisticsFactory;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   1.8.9 RenderPlayer.handleRotationFloat wrapped super.handleRotationFloat with
//   render.before/afterHandleRotationFloat, which add/subtract statistics.ticksRiding from
//   entityplayer.ticksExisted so the riding lean animation ignores the time spent mounted.
//   In 1.20.1 the equivalent is LivingEntityRenderer.getBob (ticksExisted -> tickCount), and it
//   is NOT overridden in PlayerRenderer, so the hook lives here, gated to AbstractClientPlayer.
//   This reproduces SmartRenderRender.before/afterHandleRotationFloat exactly.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
	@Inject(method = "getBob", at = @At("HEAD"))
	private void smartrender$beforeBob(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir)
	{
		if (entity instanceof AbstractClientPlayer player)
		{
			SmartStatistics statistics = SmartStatisticsFactory.getInstance(player);
			if (statistics != null)
				player.tickCount += statistics.ticksRiding;
		}
	}

	@Inject(method = "getBob", at = @At("RETURN"))
	private void smartrender$afterBob(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir)
	{
		if (entity instanceof AbstractClientPlayer player)
		{
			SmartStatistics statistics = SmartStatisticsFactory.getInstance(player);
			if (statistics != null)
				player.tickCount -= statistics.ticksRiding;
		}
	}
}
