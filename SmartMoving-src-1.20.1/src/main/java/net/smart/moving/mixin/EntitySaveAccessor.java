package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;

@Mixin(Entity.class)
public interface EntitySaveAccessor {
	@Invoker("addAdditionalSaveData")
	void smartmoving$addAdditionalSaveData(CompoundTag tag);
}
