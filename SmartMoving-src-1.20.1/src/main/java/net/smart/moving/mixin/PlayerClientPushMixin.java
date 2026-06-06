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
//
// Client-only mixin for net.minecraft.world.entity.player.Player.
//
// 1.20.1 port of the PlayerAPI client-base override:
//   public boolean pushOutOfBlocks(double d, double d1, double d2) { return moving.pushOutOfBlocks(d, d1, d2); }
// 1.8.9 Entity.pushOutOfBlocks(x,y,z) is Player.moveTowardsClosestSpace(x,z) in
// 1.20.1, invoked four times (the bounding-box corners) from Player.aiStep.
// Vanilla derives the column via BlockPos.containing(x, this.getY(), z); the
// ported SmartMovingSelf.pushOutOfBlocks(x,y,z) uses BlockPos.containing(x,y,z)
// the same way, so getY() is the faithful Y. The override fully replaced vanilla,
// so we run Smart Moving's push-out and cancel. Guarded by instanceof LocalPlayer
// and registered client-only.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(LocalPlayer.class)
public abstract class PlayerClientPushMixin
{
	@Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$moveTowardsClosestSpace(double x, double z, CallbackInfo ci)
	{
		Object self = this;
		IEntityPlayerSP isp = (IEntityPlayerSP)self;
		SmartMoving moving = isp.getMoving();
		if(moving instanceof SmartMovingSelf)
		{
			((SmartMovingSelf)moving).pushOutOfBlocks(x, ((LocalPlayer)self).getY(), z);
			ci.cancel();
		}
	}
}
