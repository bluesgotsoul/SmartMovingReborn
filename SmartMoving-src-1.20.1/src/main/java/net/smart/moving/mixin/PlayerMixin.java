// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// Shared base-class mixin for net.minecraft.world.entity.player.Player.
//
// Player.aiStep() is the 1.20.1 equivalent of EntityLivingBase.onLivingUpdate.
// ServerPlayer inherits it (no override) and LocalPlayer overrides it but calls
// super.aiStep(), so a single inject here fires for both sides. Dispatch is
// guarded by entity type so the correct "moving" owner is used.
//
// Player.causeFoodExhaustion(float) is the 1.20.1 equivalent of
// EntityPlayer.addExhaustion; it is rerouted through the Smart Moving server
// exhaustion hook (which calls back localAddExhaustion for the real food cost).
//
// There are no client branches here: LocalPlayer overrides aiStep() and calls
// super.aiStep(), so LocalPlayerMixin's own aiStep(HEAD/RETURN) injects already
// drive the client beforeOnLivingUpdate/afterOnLivingUpdate. Branching here too
// would double-fire on the client.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;

@Mixin(Player.class)
public abstract class PlayerMixin
{
	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void smartmoving$beforeAiStep(CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
				moving.beforeOnLivingUpdate();
		}
		// No client branch: LocalPlayer's client-side beforeOnLivingUpdate is already
		// driven by LocalPlayerMixin's own aiStep(HEAD) inject. Adding it here would
		// double-fire, since LocalPlayer.aiStep() calls super.aiStep() (this method).
	}

	@Inject(method = "aiStep()V", at = @At("RETURN"))
	private void smartmoving$afterAiStep(CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
				moving.afterOnLivingUpdate();
		}
		// No client branch: LocalPlayer's client-side afterOnLivingUpdate is already
		// driven by LocalPlayerMixin's own aiStep(RETURN) inject (see above).
	}

	@Inject(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$causeFoodExhaustion(float exhaustion, CallbackInfo ci)
	{
		Object self = this;
		if(self instanceof ServerPlayer)
		{
			SmartMovingServer moving = ((IEntityPlayerMP)self).getMoving();
			if(moving != null)
			{
				moving.addExhaustion(exhaustion);
				ci.cancel();
			}
		}
	}

	// NOTE[sleeping-hook]: beforeIsPlayerSleeping is SERVER-ONLY (only
	// SmartMovingServerPlayerBase overrode it) and the port impl already exists at
	// SmartMovingServer.beforeIsPlayerSleeping() -- it only needs a call site. The
	// 1.8.9 hook wrapped EntityPlayerMP.isPlayerSleeping(); the 1.20.1 analog is
	// LivingEntity.isSleeping(), a hot getter fired for every living entity, so
	// injecting there (guarded by instanceof ServerPlayer) is deferred to avoid a
	// per-call cost on non-players. Its sole effect -- a one-time crawl box init
	// (setMaxY) -- is already performed each tick by SmartMovingServer movement
	// processing, so deferring this is behavior-safe.
}
