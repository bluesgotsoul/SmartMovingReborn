package net.smart.render.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld") float smartrender$getSpeedOld();
    @Accessor("speedOld") void smartrender$setSpeedOld(float speedOld);
    @Accessor("speed") float smartrender$getSpeed();
    @Accessor("speed") void smartrender$setSpeed(float speed);
    @Accessor("position") float smartrender$getPosition();
    @Accessor("position") void smartrender$setPosition(float position);
}
