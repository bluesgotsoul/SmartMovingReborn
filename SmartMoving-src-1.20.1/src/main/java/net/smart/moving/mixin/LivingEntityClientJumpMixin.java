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
// 1.20.1 port of the PlayerAPI client-base override:
//   public void jump() { moving.jump(); }
// In 1.8.9 the client base fully replaced EntityLivingBase.jump() with a call to
// moving.jump() and never invoked super.jump(): Smart Moving takes over jump
// handling entirely, so moving.jump() only raises the jumpAvoided flag and the
// actual impulse is reapplied later inside the Smart Moving movement pass
// (consumed in SmartMovingSelf during travel/moveEntityWithHeading). 1.8.9
// EntityLivingBase.jump() is LivingEntity.jumpFromGround() in 1.20.1, so we run
// moving.jump() at HEAD and cancel the vanilla jump. LocalPlayer does not declare
// jumpFromGround(), so the inject targets the declaring class LivingEntity,
// guarded by instanceof LocalPlayer. Client-only registration keeps the
// LocalPlayer / SmartMovingSelf references off the dedicated server.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientJumpMixin
{
	@Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$jumpFromGround(CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			SmartMoving moving = isp.getMoving();
			if(moving instanceof SmartMovingSelf)
			{
				((SmartMovingSelf)moving).jump();
				ci.cancel();
			}
		}
	}
}
