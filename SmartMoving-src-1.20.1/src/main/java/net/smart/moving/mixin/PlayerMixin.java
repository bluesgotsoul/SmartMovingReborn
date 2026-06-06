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

package net.smart.moving.mixin;

import net.minecraft.world.entity.Pose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;
import net.smart.moving.SmartMovingContext;

@Mixin(Player.class)
public abstract class PlayerMixin {
	// Entity.canEnterPose(Pose) — protected, а PlayerMixin не наследник Entity,
	// поэтому прямой вызов self.canEnterPose(...) даёт "not visible". Берём через
	// @Shadow.
	@Shadow
	protected abstract boolean canEnterPose(Pose pose);

	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void smartmoving$beforeAiStep(CallbackInfo ci) {
		Object self = this;
		if (self instanceof ServerPlayer) {
			SmartMovingServer moving = ((IEntityPlayerMP) self).getMoving();
			if (moving != null)
				moving.beforeOnLivingUpdate();
		}
		// No client branch: LocalPlayer's client-side beforeOnLivingUpdate is already
		// driven by LocalPlayerMixin's own aiStep(HEAD) inject. Adding it here would
		// double-fire, since LocalPlayer.aiStep() calls super.aiStep() (this method).
	}

	@Inject(method = "aiStep()V", at = @At("RETURN"))
	private void smartmoving$afterAiStep(CallbackInfo ci) {
		Object self = this;
		if (self instanceof ServerPlayer) {
			SmartMovingServer moving = ((IEntityPlayerMP) self).getMoving();
			if (moving != null)
				moving.afterOnLivingUpdate();
		}
		// No client branch: LocalPlayer's client-side afterOnLivingUpdate is already
		// driven by LocalPlayerMixin's own aiStep(RETURN) inject (see above).
	}

	@Inject(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$causeFoodExhaustion(float exhaustion, CallbackInfo ci) {
		Object self = this;
		if (self instanceof ServerPlayer) {
			SmartMovingServer moving = ((IEntityPlayerMP) self).getMoving();
			if (moving != null) {
				moving.addExhaustion(exhaustion);
				ci.cancel();
			}
		}
	}

	@Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
	private void smartmoving$keepEyeHeightWhileSwimming(Pose pose, EntityDimensions dimensions,
			CallbackInfoReturnable<Float> cir) {
		// Backstop к Шагу 9: пока Smart Moving владеет движением в воде, не даём камере
		// опуститься
		// на ванильную swim-высоту глаз (0.4). Если что-то всё же переключит игрока в
		// Pose.SWIMMING
		// хотя бы на тик — глаза остаются на нормальной высоте, isEyeInFluid не мигает,
		// а значит не
		// мигают ни подводный оверлей, ни полоска воздуха, ни пузырьки. Pose.SWIMMING —
		// единственная,
		// которую трогаем: FALL_FLYING (элитры) и SPIN_ATTACK (трезубец) оставляем
		// ванильными.
		if (SmartMovingContext.Config.enabled && pose == Pose.SWIMMING)
			cir.setReturnValue(((Player) (Object) this).isCrouching() ? 1.27F : 1.62F);
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void smartmoving$forceStablePose(CallbackInfo ci) {
		// 1.8.9 -> 1.20.1 PORT NOTE:
		// A/B test proved the hitbox toggle is owned by the POSE, not the swim FLAG:
		// pressing CTRL
		// (sprint) underwater makes vanilla pick Pose.SWIMMING, which shrinks the
		// hitbox to 0.6 and
		// makes the bounding box / camera churn every tick (and stick even after
		// releasing CTRL).
		// While Smart Moving owns water movement, we never let the player enter the
		// swim pose - we
		// keep STANDING (or CROUCHING), exactly like 1.8.9 where no swim pose existed.
		// Elytra
		// (FALL_FLYING), trident (SPIN_ATTACK) and sleeping keep their vanilla poses.
		if (!SmartMovingContext.Config.enabled)
			return;

		Player self = (Player) (Object) this;

		if (self.isFallFlying() || self.isAutoSpinAttack() || self.isSleeping())
			return;

		Pose desired = self.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING;

		// Тот же fit-фолбэк, что и в ванильном updatePlayerPose: если в выбранную позу
		// не влезаем
		// (тесные блоки), пробуем CROUCHING, и лишь в самом крайнем случае оставляем
		// ваниле решать.
		// canEnterPose у Entity protected — зовём его через @Shadow (объявлен выше), на
		// this.
		if (!canEnterPose(desired)) {
			if (canEnterPose(Pose.CROUCHING))
				desired = Pose.CROUCHING;
			else
				return; // совсем тесно — пусть отработает ванильная логика, чтобы не застрять в блоках
		}

		self.setPose(desired);
		ci.cancel();
	}

	@Inject(method = "updateSwimming()V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$suppressVanillaSwimming(CallbackInfo ci) {
		// // 1.8.9 -> 1.20.1: Minecraft 1.8.9 had NO vanilla swimming, so Smart Moving
		// owned water
		// // movement/animation alone. In 1.13+ Player.updateSwimming() sets
		// setSwimming(true) ->
		// // Pose.SWIMMING shrinks the hitbox and enables the vanilla swim stroke,
		// fighting handleSwimming
		// // and Smart Render. Suppress it while the mod is enabled.
		if (SmartMovingContext.Config.enabled) {
			((Player) (Object) this).setSwimming(false);
			ci.cancel();
		}
	}
}