// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// Shared base-class mixin for net.minecraft.world.entity.Entity.
//
// Entity.setPos(double,double,double) is the 1.20.1 equivalent of the
// setPosition hook the PlayerAPI server base wrapped with afterSetPosition.
// This is a server-only hook; the 1.8.9 client base never wrapped setPosition.
//
// Not yet injected (kept faithful as TODO markers, matching the PlayerAPI base
// methods that have no clean 1.20.1 single-point equivalent):
//   NOTE[block-helpers]:    SmartMovingSelf never overrode isEntityInsideOpaqueBlock in 1.8.9
//                           (SmartMovingPlayerBase had no body for it). No redirect needed.
//   NOTE[sneaking-redirect]: LocalPlayer.isSneaking() is intercepted in LocalPlayerMixin
//                             via @Inject smartmoving$isSneaking -> SmartMovingSelf.isSneaking().
//   NOTE[eye-height]:        SmartMovingPlayerBase had no getEyeHeight override in 1.8.9
//                           (SmartMovingSelf did not override it either). Vanilla eye-height is fine.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	@Inject(method = "setPos(DDD)V", at = @At("RETURN"))
	private void smartmoving$afterSetPos(double x, double y, double z, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
				moving.afterSetPosition(x, y, z);
		}
		// No client branch here: the 1.8.9 client PlayerAPI base
		// (SmartMovingPlayerBase) never wrapped setPosition with afterSetPosition --
		// that hook is server-only (SmartMovingServerPlayerBase). The only client
		// position hook in the original is beforeSetPositionAndRotation, a separate
		// LocalPlayer-side hook tracked in LocalPlayerMixin; wired via @Inject on LocalPlayer.absMoveTo().
	}
}
