// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// Shared base-class mixin for net.minecraft.world.entity.LivingEntity.
//
// LivingEntity.tickEffects() is the 1.20.1 equivalent of
// EntityLivingBase.updatePotionEffects. The PlayerAPI server base wrapped this
// with the moving-hunger batch hooks, and -- faithful to the original -- the
// before/after assignment is intentionally swapped:
//   beforeUpdatePotionEffects -> moving.afterAddMovingHungerBatch()
//   afterUpdatePotionEffects  -> moving.beforeAddMovingHungerBatch()
//
// Dispatch is guarded by entity type; there is no client branch -- the
// moving-hunger batch is server-only and the client base has no equivalent.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
	@Inject(method = "tickEffects()V", at = @At("HEAD"))
	private void smartmoving$beforeTickEffects(CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
				moving.afterAddMovingHungerBatch();
		}
		// No client branch: the moving-hunger batch is server-only (the 1.8.9 client
		// base never wrapped updatePotionEffects with a hunger hook).
	}

	@Inject(method = "tickEffects()V", at = @At("RETURN"))
	private void smartmoving$afterTickEffects(CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
				moving.beforeAddMovingHungerBatch();
		}
		// No client branch (server-only; see above).
	}
}
