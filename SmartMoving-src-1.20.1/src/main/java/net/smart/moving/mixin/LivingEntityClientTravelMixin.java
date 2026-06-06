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
// Client-only base-class mixin for net.minecraft.world.entity.LivingEntity.
//
// 1.20.1 port of the movement override that the PlayerAPI client base
// (SmartMovingPlayerBase) performed:
//   public void moveEntityWithHeading(float f, float f1) { moving.moveEntityWithHeading(f, f1); }
// This override fully REPLACED vanilla moveEntityWithHeading (PlayerAPI installs
// the override, so the vanilla body never ran). SmartMovingSelf.superMoveEntityWithHeading
// is a complete reimplementation (handleSwimming/handleLava/handleAlternativeFlying/
// handleLand + jump/wall-jump/exhaustion) and never calls back into vanilla, so there
// is no localMoveEntityWithHeading bridge in the original either.
//
// In 1.20.1 vanilla moveEntityWithHeading(moveStrafing, moveForward) is
// LivingEntity.travel(Vec3) which the aiStep call site invokes as
//   travel(new Vec3(xxa, yya, zza))
// i.e. travelVector.x == moveStrafing and travelVector.z == moveForward (the
// vertical yya was never a parameter of the 1.8.9 signature, so it is dropped).
// We inject @At HEAD, run moving.moveEntityWithHeading(strafe, forward) and
// ci.cancel() so the vanilla travel body does not also run -- a faithful 1:1 of
// the PlayerAPI override. The custom physics calls sp.move(...) internally, which
// in turn is wrapped by EntityClientMoveMixin (before/afterMoveEntity).
//
// LocalPlayer does not declare travel(), so the inject must target the declaring
// class LivingEntity. Dispatch is guarded by instanceof LocalPlayer (matching the
// EntityMixin / EntityClientMoveMixin pattern) so only the local player is
// affected. This mixin is registered client-only, so referencing LocalPlayer /
// SmartMovingSelf here is safe (never loaded on a dedicated server).

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientTravelMixin
{
	@Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$travel(Vec3 travelVector, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			SmartMoving moving = isp.getMoving();
			if(moving instanceof SmartMovingSelf)
			{
				((SmartMovingSelf)moving).moveEntityWithHeading((float)travelVector.x, (float)travelVector.z);
				ci.cancel();
			}
		}
	}
}
