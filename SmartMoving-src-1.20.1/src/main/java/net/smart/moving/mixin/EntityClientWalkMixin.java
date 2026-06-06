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
// Client-only base-class mixin for net.minecraft.world.entity.Entity.
//
// 1.20.1 port of the PlayerAPI client-base override:
//   public boolean canTriggerWalking() { return moving.canTriggerWalking(); }
// 1.8.9 Entity.canTriggerWalking() is Entity.isMovementNoisy() in 1.20.1 (it
// gates walk-distance / step-sound accounting in Entity.move). The override fully
// replaced the vanilla test with Smart Moving's (!isClimbing && !isDiving) logic,
// so we set the return value and cancel. Guarded by instanceof LocalPlayer and
// registered client-only so the client references are server-safe.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(Entity.class)
public abstract class EntityClientWalkMixin
{
	@Inject(method = "isSteppingCarefully()Z", at = @At("HEAD"), cancellable = true)
	private void smartmoving$isSteppingCarefully(CallbackInfoReturnable<Boolean> cir)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			SmartMoving moving = isp.getMoving();
			if(moving instanceof SmartMovingSelf)
				cir.setReturnValue(((SmartMovingSelf)moving).canTriggerWalking());
		}
	}
}
