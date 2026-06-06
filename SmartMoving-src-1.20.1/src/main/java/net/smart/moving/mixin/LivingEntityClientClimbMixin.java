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
//   public boolean isOnLadder() { return moving.isOnLadderOrVine(); }
// 1.8.9 Entity.isOnLadder() is LivingEntity.onClimbable() in 1.20.1. The override
// fully replaced the vanilla climb test with Smart Moving's ladder/vine logic, so
// we set the return value and cancel. LocalPlayer does not declare onClimbable(),
// so the inject targets the declaring class LivingEntity, guarded by instanceof
// LocalPlayer. Client-only registration keeps the LocalPlayer / SmartMovingSelf
// references off the dedicated server.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientClimbMixin
{
	@Inject(method = "onClimbable()Z", at = @At("HEAD"), cancellable = true)
	private void smartmoving$onClimbable(CallbackInfoReturnable<Boolean> cir)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			SmartMoving moving = isp.getMoving();
			if(moving instanceof SmartMovingSelf)
				cir.setReturnValue(((SmartMovingSelf)moving).isOnLadderOrVine());
		}
	}
}
