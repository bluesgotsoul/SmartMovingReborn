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
// 1.20.1 port note: client-side player accessor interface (was implemented by
// the PlayerAPI client player base SmartMovingPlayerBase). In 1.20.1 it is
// implemented by the LocalPlayer mixin. "local*" methods invoke the real
// vanilla behaviour, the non-local hooks route through SmartMoving.
//
// Type mappings vs 1.8.9:
//   Material            -> TagKey<Fluid> (only water/lava were ever used)
//   NBTTagCompound      -> CompoundTag
//   EnumStatus          -> Either<Player.BedSleepingProblem, Unit> (startSleepInBed)
//   sleepInBedAt(i,j,k) -> startSleepInBed(BlockPos)

package net.smart.moving;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

public interface IEntityPlayerSP
{
	SmartMoving getMoving();

	// Reentrancy guard mirroring PlayerAPI's super.moveEntity bypass. True while
	// localMoveEntity performs the raw vanilla move, so EntityClientMoveMixin
	// skips the beforeMoveEntity/afterMoveEntity wrap for that inner step.
	boolean isInLocalMove();

	// Reentrancy guard for the FOV hook (ComputeFovModifierEvent). True while
	// localGetFOVMultiplier reads the raw vanilla getFieldOfViewModifier().
	boolean isInFovQuery();

	boolean getSleepingField();

	boolean getIsJumpingField();

	// 1.8.9 isInWeb field. In 1.20.1 cobweb sets a movement multiplier
	// (stuckSpeedMultiplier / makeStuckInBlock); the boolean is emulated by the
	// LocalPlayer mixin.
	boolean getIsInWebField();

	void setIsInWebField(boolean b);

	Minecraft getMcField();

	void setMoveForwardField(float f);

	void setMoveStrafingField(float f);

	void setIsJumpingField(boolean flag);

	// 1.8.9 moveEntity -> 1.20.1 move(MoverType.SELF, Vec3)
	void localMoveEntity(double d, double d1, double d2);

	// 1.8.9 sleepInBedAt(i,j,k):EnumStatus -> 1.20.1 startSleepInBed(BlockPos)
	Either<Player.BedSleepingProblem, Unit> localSleepInBedAt(BlockPos pos);

	float localGetBrightness(float f);

	int localGetBrightnessForRender(float f);

	void localUpdateEntityActionState();

	// 1.8.9 isInsideOfMaterial(Material) -> 1.20.1 fluid-tag membership
	boolean localIsInsideOfMaterial(TagKey<Fluid> fluidTag);

	void localWriteEntityToNBT(CompoundTag compoundTag);

	boolean localIsSneaking();

	float localGetFOVMultiplier();
}
