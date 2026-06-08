// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// 1.20.1 port of the client-side half of the PlayerAPI base
// net.smart.moving.playerapi.SmartMovingPlayerBase.
//
// PlayerAPI no longer exists, so LocalPlayer now directly implements
// IEntityPlayerSP via this mixin and owns its client SmartMovingSelf instance
// ("moving"). The before/after lifecycle hooks the client base used are
// recreated as @Inject points on LocalPlayer's own overrides (tick / aiStep),
// so they call moving.* directly -- the shared base-class mixins
// (PlayerMixin / EntityMixin / LivingEntityMixin) stay server-only because all
// client hooks live on SmartMovingSelf, not the SmartMoving base.
//
// local* methods reproduce the vanilla behaviour the original obtained via
// super.*; in a mixin those vanilla calls require redirect/accessor handling
// without re-entering our own hooks, so the ones that cannot be expressed
// cleanly yet are marked NOTE[reentrancy] / NOTE[client-hooks] and the
// physics-dependent body hooks are added with the SmartMovingSelf physics pass.
//
// 1.8.9 -> 1.20.1 field/type mappings used here:
//   player.moveForward / moveStrafing -> LivingEntity.zza / xxa
//   isJumping field                   -> LivingEntity.jumping (protected)
//   Minecraft.getMinecraft()          -> Minecraft.getInstance()
//   sleeping field                    -> Player.isSleeping()
//   super.isSneaking()                -> isShiftKeyDown()

package net.smart.moving.mixin;

import com.mojang.datafixers.util.Either;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.LightLayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements IEntityPlayerSP {
	// Inherited from LivingEntity; shadowed so the input-field accessors below
	// map exactly to the 1.8.9 moveForward / moveStrafing / isJumping fields.
	// Inherited fields are accessed via LivingEntityAccessor/EntitySaveAccessor

	@Unique
	private SmartMovingSelf smartmoving$moving;

	// Reentrancy guard mirroring PlayerAPI's super.moveEntity bypass: while set,
	// EntityClientMoveMixin skips the beforeMoveEntity/afterMoveEntity wrap so
	// localMoveEntity performs a raw vanilla move.
	@Unique
	private boolean smartmoving$inLocalMove;

	// Saved cobweb slowdown for the 1.8.9 isInWeb save/restore. 1.20.1 has no
	// isInWeb boolean -- cobweb sets Entity.stuckSpeedMultiplier (a Vec3) instead.
	// We emulate the boolean via EntityStuckAccessor and keep the exact multiplier
	// here so the relocate-move save -> clear -> restore sequence is lossless.
	@Unique
	private Vec3 smartmoving$savedStuck;

	// Reentrancy guard for the FOV hook: while set, the ComputeFovModifierEvent
	// listener leaves the value alone so localGetFOVMultiplier can read the raw
	// vanilla getFieldOfViewModifier() (the 1.8.9 super.getFovModifier() value).

	@Unique
	private boolean smartmoving$inSneakQuery;
	@Unique
	private boolean smartmoving$inFovQuery;

	@Unique
	private LocalPlayer smartmoving$self() {
		return (LocalPlayer) (Object) this;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void smartmoving$init(CallbackInfo ci) {
		smartmoving$moving = new SmartMovingSelf(smartmoving$self(), this);
	}

	// ------------------------------------------------------------------
	// Client lifecycle hooks (LocalPlayer's own overrides -> moving.*)
	// ------------------------------------------------------------------

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void smartmoving$beforeTick(CallbackInfo ci) {
		if (smartmoving$moving != null)
			smartmoving$moving.beforeOnUpdate();
	}

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void smartmoving$afterTick(CallbackInfo ci) {
		if (smartmoving$moving != null)
			smartmoving$moving.afterOnUpdate();
	}

	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void smartmoving$beforeAiStep(CallbackInfo ci) {
		if (smartmoving$moving != null)
			smartmoving$moving.beforeOnLivingUpdate();
	}

	@Inject(method = "aiStep()V", at = @At("RETURN"))
	private void smartmoving$afterAiStep(CallbackInfo ci) {
		if (smartmoving$moving != null)
			smartmoving$moving.afterOnLivingUpdate();
	}

	// NOTE[client-hooks]: remaining method-wrap hooks from SmartMovingPlayerBase
	// route through SmartMovingSelf and depend on the physics pass; they will be
	// added as @Inject/@Redirect when SmartMovingSelf bodies are implemented:
	// (beforeMoveEntity / afterMoveEntity on move(MoverType,Vec3): wired by
	// EntityClientMoveMixin)
	// (moveEntityWithHeading on travel(Vec3): wired by
	// LivingEntityClientTravelMixin)
	// (canTriggerWalking on isMovementNoisy: wired by EntityClientWalkMixin)
	// (isOnLadder on onClimbable: wired by LivingEntityClientClimbMixin)
	// (pushOutOfBlocks on moveTowardsClosestSpace: wired by PlayerClientPushMixin)
	// getBrightness / getBrightnessForRender
	// isInsideOfMaterial (fluid tag)
	// writeEntityToNBT (addAdditionalSaveData)
	// (isSneaking: wired above via @Inject on LocalPlayer.isSneaking() ->
	// SmartMovingSelf.isSneaking())
	// getFovModifier (Forge ComputeFovModifierEvent: wired by
	// SmartMovingClient.onComputeFovModifier + localGetFOVMultiplier below)
	// (beforeTrySleep -> moving.beforeSleepInBedAt(pos) on startSleepInBed: wired
	// by PlayerClientSleepMixin)
	// (jump -> moving.jump() on jumpFromGround: wired by
	// LivingEntityClientJumpMixin)
	// beforeGetSleepTimer (renderGuiIngame sleep-timer overlay -> Phase C render
	// layer)
	// beforeSetPositionAndRotation -> moving.beforeSetPositionAndRotation() (client
	// re-init on
	// network position sync). Candidate target Entity.absMoveTo(DDDFF)V (old
	// setPositionAndRotation),
	// but the 1.20.1 ClientPacketListener position-packet call site (absMoveTo vs
	// moveTo) is not yet
	// verified; left unwired to avoid a silently-wrong target rather than guess.

	// ------------------------------------------------------------------
	// SmartMoving isSneaking redirect
	// 1.8.9: SmartMovingPlayerBase.isSneaking() -> moving.isSneaking().
	// 1.20.1: @Inject HEAD/cancellable on LocalPlayer.isSneaking() (inherited
	// from LivingEntity) routes through SmartMovingSelf.isSneaking() so vanilla
	// hitbox/render code sees the SmartMoving-adjusted sneaking state.
	// ------------------------------------------------------------------
	@Inject(method = "isShiftKeyDown()Z", at = @At("HEAD"), cancellable = true)
	private void smartmoving$isShiftKeyDown(CallbackInfoReturnable cir) {
		// Если мы тут из-за localIsSneaking() — не перехватываем: пусть отработает
		// RAW-ванильный
		// isShiftKeyDown() (чистое состояние клавиши = 1.8.9 super.isSneaking()). Иначе
		// isSneaking()
		// в ветке ((sp.isPassenger() || !Config.enabled) && isp.localIsSneaking())
		// снова влетит сюда
		// -> localIsSneaking() -> сюда -> бесконечная рекурсия -> StackOverflowError.
		if (smartmoving$inSneakQuery)
			return;

		SmartMoving moving = smartmoving$moving;
		if (moving instanceof SmartMovingSelf)
			cir.setReturnValue(((SmartMovingSelf) moving).isSneaking());
	}
	// ------------------------------------------------------------------
	// IEntityPlayerSP
	// ------------------------------------------------------------------

	@Override
	public SmartMoving getMoving() {
		return smartmoving$moving;
	}

	@Override
	public boolean getSleepingField() {
		return smartmoving$self().isSleeping();
	}

	@Override
	public boolean getIsJumpingField() {
		return ((LivingEntityAccessor) this).smartmoving$getJumping();
	}

	@Override
	public void setIsJumpingField(boolean flag) {
		((LivingEntityAccessor) this).smartmoving$setJumping(flag);
	}

	@Override
	public boolean getIsInWebField() {
		// 1.8.9 Entity.isInWeb -> 1.20.1 Entity.stuckSpeedMultiplier (cobweb sets it
		// non-zero via makeStuckInBlock); "in web" iff the multiplier is non-zero.
		Vec3 multiplier = ((EntityStuckAccessor) (Object) this).smartmoving$getStuckSpeedMultiplier();
		return multiplier != null && multiplier.lengthSqr() > 1.0E-7D;
	}

	@Override
	public void setIsInWebField(boolean b) {
		// Mirror the 1.8.9 isInWeb writes (save -> clear -> restore). Clearing stores
		// the current multiplier so it can be restored byte-for-byte; setting true
		// restores the saved value, falling back to the vanilla cobweb multiplier.
		EntityStuckAccessor accessor = (EntityStuckAccessor) (Object) this;
		if (b) {
			Vec3 restore = smartmoving$savedStuck;
			if (restore == null || restore.lengthSqr() <= 1.0E-7D)
				restore = new Vec3(0.25D, (double) 0.05F, 0.25D);
			accessor.smartmoving$setStuckSpeedMultiplier(restore);
		} else {
			smartmoving$savedStuck = accessor.smartmoving$getStuckSpeedMultiplier();
			accessor.smartmoving$setStuckSpeedMultiplier(Vec3.ZERO);
		}
	}

	@Override
	public Minecraft getMcField() {
		return Minecraft.getInstance();
	}

	@Override
	public void setMoveForwardField(float f) {
		((LivingEntityAccessor) this).smartmoving$setZza(f);
	}

	@Override
	public void setMoveStrafingField(float f) {
		((LivingEntityAccessor) this).smartmoving$setXxa(f);
	}

	@Override
	public void localMoveEntity(double d, double d1, double d2) {
		// 1.8.9 super.moveEntity(d,d1,d2) -> raw vanilla move(MoverType.SELF, Vec3).
		// The guard keeps EntityClientMoveMixin from re-wrapping this raw move with
		// beforeMoveEntity/afterMoveEntity (matches PlayerAPI's super bypass).
		smartmoving$inLocalMove = true;
		try {
			smartmoving$self().move(MoverType.SELF, new Vec3(d, d1, d2));
		} finally {
			smartmoving$inLocalMove = false;
		}
	}

	@Override
	public boolean isInLocalMove() {
		return smartmoving$inLocalMove;
	}

	@Override
	public Either<Player.BedSleepingProblem, Unit> localSleepInBedAt(BlockPos pos) {
		// 1.8.9 super.trySleep(pos) -> 1.20.1 Player.startSleepInBed(BlockPos)
		return smartmoving$self().startSleepInBed(pos);
	}

	@Override
	public float localGetBrightness(float f) {
		// 1.8.9 super.getBrightness(f) -> 1.20.1 getLightLevelDependentMagicValue()
		return smartmoving$self().getLightLevelDependentMagicValue();
	}

	@Override
	public int localGetBrightnessForRender(float f) {
		// 1.8.9 super.getBrightnessForRender(f) returned packed (sky<<4|block).
		// 1.20.1 equivalent: LightTexture.pack(block, sky) from the level light engine.
		LocalPlayer self = smartmoving$self();
		BlockPos pos = self.blockPosition();
		int block = self.level().getBrightness(LightLayer.BLOCK, pos);
		int sky = self.level().getBrightness(LightLayer.SKY, pos);
		return LightTexture.pack(block, sky);
	}

	@Override
	public void localUpdateEntityActionState() {
		// NOTE[client-hooks]: the vanilla aiStep() code (the 1.20.1 equivalent of
		// updateEntityActionState) already ran before this point -- the inject fires
		// at aiStep() RETURN. No re-entry needed; deliberate no-op.
	}

	@Override
	public boolean localIsInsideOfMaterial(TagKey<Fluid> fluidTag) {
		// 1.8.9 super.isInsideOfMaterial(material) -> 1.20.1 eye-in-fluid-tag check
		return smartmoving$self().isEyeInFluid(fluidTag);
	}

	@Override
	public void localWriteEntityToNBT(CompoundTag compoundTag) {
		// 1.8.9 super.writeEntityToNBT(tag) -> 1.20.1
		// Entity.addAdditionalSaveData(CompoundTag)
		((EntitySaveAccessor) this).smartmoving$addAdditionalSaveData(compoundTag);
	}

	@Override
	public boolean localIsSneaking() {
		// 1.8.9 super.isSneaking() = RAW-ванильное состояние шифта. Guard глушит наш
		// собственный
		// isShiftKeyDown()-хук на время этого вызова, иначе он уведёт обратно в
		// SmartMovingSelf.isSneaking()
		// (рекурсия -> StackOverflow). Тот же паттерн, что smartmoving$inFovQuery /
		// smartmoving$inLocalMove.
		smartmoving$inSneakQuery = true;
		try {
			return smartmoving$self().isShiftKeyDown();
		} finally {
			smartmoving$inSneakQuery = false;
		}
	}

	@Override
	public float localGetFOVMultiplier() {
		// 1.8.9 super.getFovModifier() -> 1.20.1
		// AbstractClientPlayer.getFieldOfViewModifier().
		// The guard makes the ComputeFovModifierEvent listener leave this inner
		// vanilla computation alone, mirroring PlayerAPI's super-call bypass.
		smartmoving$inFovQuery = true;
		try {
			return smartmoving$self().getFieldOfViewModifier();
		} finally {
			smartmoving$inFovQuery = false;
		}
	}

	@Override
	public boolean isInFovQuery() {
		return smartmoving$inFovQuery;
	}
}
