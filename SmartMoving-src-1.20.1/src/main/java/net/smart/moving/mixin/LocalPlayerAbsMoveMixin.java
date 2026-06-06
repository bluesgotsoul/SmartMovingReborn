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
// 1.20.1 port of the PlayerAPI client-base hook:
//   SmartMovingPlayerBase.beforeSetPositionAndRotation(double,double,double,float,float)
//     -> moving.beforeSetPositionAndRotation()
//
// In 1.8.9 the PlayerAPI client base wrapped Entity.setPositionAndRotation
// (called by the client-side network handler when the server forces a position
// correction onto LocalPlayer). SmartMovingSelf.beforeSetPositionAndRotation
// resets 'initialized' and 'multiPlayerInitialized' so the movement state is
// re-derived on the next tick after the teleport/correction.
//
// 1.20.1 equivalent: Entity.absMoveTo(double x, double y, double z,
//                                      float yRot, float xRot)  (was setPositionAndRotation).
// LocalPlayer does not override absMoveTo, so the mixin targets Entity and is
// guarded by instanceof LocalPlayer. Client-only registration (LocalPlayer and
// SmartMovingSelf must not load on a dedicated server).

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(Entity.class)
public abstract class LocalPlayerAbsMoveMixin
{
	@Inject(method = "absMoveTo(DDDFF)V", at = @At("HEAD"))
	private void smartmoving$beforeSetPositionAndRotation(double x, double y, double z, float yRot, float xRot, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			SmartMoving moving = ((IEntityPlayerSP)self).getMoving();
			if(moving instanceof SmartMovingSelf)
				((SmartMovingSelf)moving).beforeSetPositionAndRotation();
		}
	}
}
