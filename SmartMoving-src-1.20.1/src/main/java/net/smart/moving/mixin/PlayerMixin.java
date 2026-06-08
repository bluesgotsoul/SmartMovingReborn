package net.smart.moving.mixin;

import net.minecraft.world.entity.Pose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingServer;
import net.smart.moving.SmartMovingContext;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void smartmoving$beforeAiStep(CallbackInfo ci) {
		Object self = this;
		if (self instanceof ServerPlayer) {
			SmartMovingServer moving = ((IEntityPlayerMP) self).getMoving();
			if (moving != null)
				moving.beforeOnLivingUpdate();
		}
	}

	@Inject(method = "aiStep()V", at = @At("RETURN"))
	private void smartmoving$afterAiStep(CallbackInfo ci) {
		Object self = this;
		if (self instanceof ServerPlayer) {
			SmartMovingServer moving = ((IEntityPlayerMP) self).getMoving();
			if (moving != null)
				moving.afterOnLivingUpdate();
		}
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
		// Backstop к Шагу 9: не даём камере опуститься на ванильную swim-высоту глаз
		// (0.4).
		if (SmartMovingContext.Config.enabled && pose == Pose.SWIMMING)
			cir.setReturnValue(((Player) (Object) this).isCrouching() ? 1.27F : 1.62F);
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	private void smartmoving$reduceHitbox(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		// 1.8.9 -> 1.20.1 PORT NOTE:
		// 1.8.9 did `sp.height += heightOffset` (e.g. 1.8 -> 0.8 at heightOffset=-1),
		// genuinely shrinking
		// the COLLISION size for flight/crawl/slide/swim/dive. The port dropped that
		// and only nudged the
		// AABB (overwritten every move by the dimension-driven box). We restore the
		// real shrink by returning
		// reduced EntityDimensions, so the 1.20.1 box (computed from dimensions) is
		// actually shorter.
		if (!SmartMovingContext.Config.enabled)
			return;

		Object self = this;
		if (!(self instanceof IEntityPlayerSP))
			return; // только локальный игрок владеет SmartMovingSelf

		SmartMoving moving = ((IEntityPlayerSP) self).getMoving();
		if (moving == null || moving.heightOffset == 0F)
			return; // обычное состояние -> ванильные габариты позы

		// 0.6 x 1.8 — ванильные габариты стоящего игрока; высота = 1.8 + heightOffset
		// (0.8 при -1), как в 1.8.9.
		float height = 1.8F + moving.heightOffset;
		if (height < 0.2F)
			height = 0.2F;
		cir.setReturnValue(EntityDimensions.scalable(0.6F, height));
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void smartmoving$forceStablePose(CallbackInfo ci) {
		// Пока Smart Moving владеет движением в воде — никогда не входим в
		// Pose.SWIMMING:
		// держим STANDING (или CROUCHING). Элитры/трезубец/сон оставляем ванильными.
		if (!SmartMovingContext.Config.enabled)
			return;

		Player self = (Player) (Object) this;

		if (self.isFallFlying() || self.isAutoSpinAttack() || self.isSleeping())
			return;

		Pose desired = self.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING;

		// fit-фолбэк как в ванильном updatePlayerPose. ВАЖНО: НЕ зовём canEnterPose и
		// НЕ шадоуим его —
		// @Shadow на m_20175_ падает при запуске («was not located in the target class
		// Player»), т.к.
		// canEnterPose объявлен в Entity, а не в Player. Повторяем проверку публичным
		// API (см. ниже).
		if (!smartmoving$fits(self, desired)) {
			if (smartmoving$fits(self, Pose.CROUCHING))
				desired = Pose.CROUCHING;
			else
				return; // совсем тесно — пусть отработает ванильная логика, чтобы не застрять в блоках
		}

		self.setPose(desired);
		ci.cancel();
	}

	// Точная реплика Entity.canEnterPose(Pose) на ПУБЛИЧНОМ API — без @Shadow,
	// поэтому ремап не падает.
	private static boolean smartmoving$fits(Player self, Pose pose) {
		EntityDimensions dim = self.getDimensions(pose);
		AABB box = dim.makeBoundingBox(self.getX(), self.getY(), self.getZ()).deflate(1.0E-7D);
		return self.level().noCollision(self, box);
	}

	@Inject(method = "updateSwimming()V", at = @At("HEAD"), cancellable = true)
	private void smartmoving$suppressVanillaSwimming(CallbackInfo ci) {
		// Гасим ванильный swim-флаг (Шаг 9): один владелец плавания — Smart Moving, как
		// в 1.8.9.
		if (SmartMovingContext.Config.enabled) {
			((Player) (Object) this).setSwimming(false);
			ci.cancel();
		}
	}
}