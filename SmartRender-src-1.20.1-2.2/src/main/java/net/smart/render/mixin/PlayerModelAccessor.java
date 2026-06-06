package net.smart.render.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerModel.class)
public interface PlayerModelAccessor {
    @Accessor("slim") boolean smartrender$isSlim();
    @Accessor("cloak") ModelPart smartrender$getCloak();
    @Accessor("ear") ModelPart smartrender$getEar();
}
