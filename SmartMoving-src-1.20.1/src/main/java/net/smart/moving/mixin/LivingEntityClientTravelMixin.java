// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// Client-only base-class mixin for net.minecraft.world.entity.LivingEntity.
// Replaces vanilla travel() for the local player with Smart Moving physics
// (SmartMovingSelf.moveEntityWithHeading) -- a 1:1 of the old PlayerAPI override.
//
// INPUT-STATE FIX (Step 17, corrected): 1.8.9 EntityPlayerSP.onLivingUpdate() ran
// updateEntityActionState() EVERY tick (PlayerAPI invoked it automatically), in this strict order:
//   1. movementInput updated (sneak/sprint/jump buttons)        <- LocalPlayer.aiStep() body
//   2. super.onLivingUpdate() -> updateEntityActionState()      <- jumpAvoided=false, sets `jumping`
//   3. vanilla jump block -> jump()  (-> jumpAvoided=true)       <- same super call
//   4. moveEntityWithHeading() -> handleJumping() reads jumpAvoided
// The 1.20.1 equivalent of step 2 is the HEAD of LivingEntity.aiStep(), reached from
// LocalPlayer.aiStep() via super.aiStep(): AFTER input.tick (fresh buttons) and BEFORE the jump block.
//
// The first revision called updateEntityActionState(false) at travel() HEAD instead. travel() runs
// AFTER the jump block, so updateEntityActionState's first line (jumpAvoided = false) WIPED the flag
// the vanilla jump hook had just set, right before handleJumping() read it -> the player could NEVER
// jump. It also applied the exhaustion sprint-stop (setSprinting(false)) too late to affect that
// tick's movement -> stamina drained but running never stopped.
//
// FIX: run the per-tick updateEntityActionState(false) at aiStep() HEAD (the faithful 1.8.9 spot).
// jumpAvoided now survives to handleJumping (jump works), and setSprinting(false) is applied before
// movement (exhaustion actually gates running). Crouch/crawl/climb/slide input still works (the state
// machine still runs every tick) and Step 16's flight hitbox shrink still runs (aiStep fires every
// tick, including ability-flight).
//
// FLIGHT FIX (Step 14): when getAbilities().flying we do NOT cancel vanilla travel(), so
// creative/spectator flight keeps exact vanilla speed and the sprint-fly x2 boost.

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
public abstract class LivingEntityClientTravelMixin {
	// Шаг 17 (исправлено): потиковый прогон машины состояний в ВЕРНОЙ точке 1.8.9
	// -- HEAD
	// LivingEntity.aiStep(). Сюда исполнение приходит из LocalPlayer.aiStep() через
	// super.aiStep():
	// УЖЕ после input.tick (свежие шифт/спринт/прыжок) и ДО ванильного jump-блока и
	// travel().
	// Метод опрашивает кнопки и крутит машину
	// приседа/ползания/лазания/слайда/спринта/полёта,
	// выставляет поле jumping и сбрасывает jumpAvoided=false -- ровно перед
	// jump-блоком, поэтому
	// ванильный jump-хук (jump() -> jumpAvoided=true) НЕ затирается и
	// handleJumping() срабатывает.
	// Раньше вызов стоял в HEAD travel() (после jump-блока) -> jumpAvoided=false
	// стирал флаг перед
	// чтением (прыжок не работал), а setSprinting(false) опаздывал на движение тика
	// (бег не
	// останавливался при истощении). Guard instanceof LocalPlayer: для остальных
	// LivingEntity no-op.
	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void smartmoving$aiStepActionState(CallbackInfo ci) {
		Object self = this;
		if (self instanceof LocalPlayer) {
			IEntityPlayerSP isp = (IEntityPlayerSP) self;
			SmartMoving moving = isp.getMoving();
			if (moving instanceof SmartMovingSelf)
				((SmartMovingSelf) moving).updateEntityActionState(false);
		}
	}

	@Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$travel(Vec3 travelVector, CallbackInfo ci) {
		Object self = this;
		if (self instanceof LocalPlayer) {
			// Шаг 14: креатив/спектатор-полёт по способностям -> движение отдаём ванильному
			// travel()
			// (точная скорость полёта и спринт-буст x2). Состояние действий уже обновлено в
			// smartmoving$aiStepActionState выше (этот же тик).
			if (((LocalPlayer) self).getAbilities().flying)
				return;

			IEntityPlayerSP isp = (IEntityPlayerSP) self;
			SmartMoving moving = isp.getMoving();
			if (moving instanceof SmartMovingSelf) {
				((SmartMovingSelf) moving).moveEntityWithHeading((float) travelVector.x, (float) travelVector.z);
				ci.cancel();
			}
		}
	}
}