// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Moving is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Moving. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================
//
// Accessor for net.minecraft.world.entity.Entity#stuckSpeedMultiplier.
//
// 1.8.9 tracked cobweb state with a boolean Entity.isInWeb field, exposed through
// IEntityPlayerSP.getIsInWebField()/setIsInWebField(). 1.20.1 replaced that flag
// with a private Vec3 stuckSpeedMultiplier (set by makeStuckInBlock; cobweb uses
// new Vec3(0.25, 0.05, 0.25)). The field is private, so this accessor exposes it
// so LocalPlayerMixin can emulate the old isInWeb boolean faithfully. Entity and
// Vec3 exist on both sides, so this accessor is registered as a common mixin.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public interface EntityStuckAccessor
{
	@Accessor("stuckSpeedMultiplier")
	Vec3 smartmoving$getStuckSpeedMultiplier();

	@Accessor("stuckSpeedMultiplier")
	void smartmoving$setStuckSpeedMultiplier(Vec3 stuckSpeedMultiplier);
}
