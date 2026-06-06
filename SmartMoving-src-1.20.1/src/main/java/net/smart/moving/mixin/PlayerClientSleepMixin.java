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
// Client-only base-class mixin for net.minecraft.world.entity.player.Player.
//
// 1.20.1 port of the PlayerAPI client-base "before" hook:
//   public void beforeTrySleep(BlockPos pos) { moving.beforeSleepInBedAt(pos.getX(), pos.getY(), pos.getZ()); }
// 1.8.9 EntityPlayer.trySleep(BlockPos) is Player.startSleepInBed(BlockPos) in
// 1.20.1 (the local sleep entry point wrapped by localSleepInBedAt). The hook
// only runs the Smart Moving pre-sleep setup and does not alter the result, so we
// inject at HEAD without cancelling. The original override lived on the CLIENT
// player base only (the server base has no equivalent), so the inject targets the
// declaring class Player guarded by instanceof LocalPlayer, with client-only
// registration keeping LocalPlayer / SmartMovingSelf off the dedicated server.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(Player.class)
public abstract class PlayerClientSleepMixin
{
	// startSleepInBed returns Either<Player.BedSleepingProblem, Unit>; Mixin requires a
	// CallbackInfoReturnable for non-void targets even though this HEAD hook neither
	// reads nor sets the return value. The raw type avoids importing the Either/Unit
	// generics solely to satisfy the signature.
	@SuppressWarnings("rawtypes")
	@Inject(method = "startSleepInBed", at = @At("HEAD"))
	private void smartmoving$beforeStartSleepInBed(BlockPos pos, CallbackInfoReturnable cir)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			SmartMoving moving = isp.getMoving();
			if(moving instanceof SmartMovingSelf)
				((SmartMovingSelf)moving).beforeSleepInBedAt(pos);
		}
	}
}
