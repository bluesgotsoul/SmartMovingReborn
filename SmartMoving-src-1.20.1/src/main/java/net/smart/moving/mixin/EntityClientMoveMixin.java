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
// 1.20.1 port of the move wrapping that the PlayerAPI client base
// (SmartMovingPlayerBase) performed. Its moveEntity override ran:
//   beforeMoveEntity(d,d1,d2) -> super.moveEntity (raw) -> afterMoveEntity(d,d1,d2)
// so every displacement of the local player was wrapped, while the internal
// raw step (super.moveEntity == IEntityPlayerSP.localMoveEntity) stayed
// unwrapped. PlayerAPI is gone and LocalPlayer does not override
// Entity.move(MoverType,Vec3), so the wrapping is recreated here: @At HEAD is
// beforeMoveEntity, the vanilla move body is the raw displacement, @At RETURN
// is afterMoveEntity.
//
// Dispatch is guarded by instanceof LocalPlayer (matching the EntityMixin
// server pattern) so only the local player is affected, and by
// IEntityPlayerSP.isInLocalMove() so the raw localMoveEntity path is not
// re-wrapped. This mixin is registered client-only, so referencing LocalPlayer
// / SmartMovingSelf here is safe (never loaded on a dedicated server).

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(Entity.class)
public abstract class EntityClientMoveMixin
{
	@Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
	private void smartmoving$beforeMove(MoverType type, Vec3 movement, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			if(!isp.isInLocalMove())
			{
				SmartMoving moving = isp.getMoving();
				if(moving instanceof SmartMovingSelf)
					((SmartMovingSelf)moving).beforeMoveEntity(movement.x, movement.y, movement.z);
			}
		}
	}

	@Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
	private void smartmoving$afterMove(MoverType type, Vec3 movement, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof LocalPlayer)
		{
			IEntityPlayerSP isp = (IEntityPlayerSP)self;
			if(!isp.isInLocalMove())
			{
				SmartMoving moving = isp.getMoving();
				if(moving instanceof SmartMovingSelf)
					((SmartMovingSelf)moving).afterMoveEntity(movement.x, movement.y, movement.z);
			}
		}
	}
}
