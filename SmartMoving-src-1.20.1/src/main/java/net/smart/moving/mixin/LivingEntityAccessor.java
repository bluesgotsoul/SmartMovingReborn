package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("zza")
	float smartmoving$getZza();

	@Accessor("zza")
	void smartmoving$setZza(float zza);

	@Accessor("xxa")
	float smartmoving$getXxa();

	@Accessor("xxa")
	void smartmoving$setXxa(float xxa);

	@Accessor("jumping")
	boolean smartmoving$getJumping();

	@Accessor("jumping")
	void smartmoving$setJumping(boolean jumping);
}
