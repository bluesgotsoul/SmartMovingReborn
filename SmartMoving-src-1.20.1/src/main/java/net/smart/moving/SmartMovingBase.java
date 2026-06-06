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
// 1.20.1 PORT - PHYSICS CORE (scaffold).
// This is the world/collision/liquid/climb query layer shared by
// SmartMovingSelf (client) and SmartMovingOther (other players).
//
// Per the agreed "scaffold first, then logic in layers" approach, the simple
// and cleanly-portable helpers were implemented first; the heavy physics queries
// (climb/ladder/vine, liquid borders, solid/liquid column scans) have since been
// ported with their real geometry math.
//
// Type mappings vs 1.8.9 (see also IEntityPlayerSP):
//   EntityPlayer       -> net.minecraft.world.entity.player.Player
//   EntityPlayerSP     -> net.minecraft.client.player.LocalPlayer
//   IBlockState        -> BlockState
//   Material           -> queried via BlockState.getFluidState()/tags; the old
//                         Material API is gone. getMaterial(...) returns the
//                         BlockState so callers can inspect fluid/tags.
//   worldObj           -> sp.level()
//   getEntityBoundingBox -> sp.getBoundingBox(); setEntityBoundingBox -> setBoundingBox
//   MathHelper.sqrt_*  -> Mth.sqrt; floor_double -> Mth.floor
//   rotationYaw/Pitch  -> getYRot()/getXRot(); motionX/Y/Z -> getDeltaMovement()/setDeltaMovement

package net.smart.moving;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.smart.moving.config.*;

public abstract class SmartMovingBase extends SmartMovingContext
{
	public final Player sp;
	public final LocalPlayer esp;
	public final IEntityPlayerSP isp;

	public SmartMovingBase(Player sp, IEntityPlayerSP isp)
	{
		this.sp = sp;
		this.isp = isp;

		if(sp instanceof LocalPlayer)
		{
			esp = (LocalPlayer)sp;
			if(Minecraft.getInstance().player == null)
			{
				Options.resetForNewGame();
				Config = Options;
			}
		}
		else
			esp = null;
	}

	// --- cleanly portable block / bounding-box accessors ---

	public Block getBlock(int x, int y, int z)
	{
		return sp.level().getBlockState(new BlockPos(x, y, z)).getBlock();
	}

	public BlockState getState(int x, int y, int z)
	{
		return sp.level().getBlockState(new BlockPos(x, y, z));
	}

	// 1.8.9 returned Material; 1.20.1 has no Material type, so we surface the
	// BlockState and let callers inspect getFluidState()/tags. Callers compare
	// against fluid tags (FluidTags.WATER / FluidTags.LAVA) instead of Material.
	public BlockState getMaterial(int x, int y, int z)
	{
		return getState(x, y, z);
	}

	public boolean isAirBlock(int x, int y, int z)
	{
		return sp.level().isEmptyBlock(new BlockPos(x, y, z));
	}

	public AABB getBoundingBox()
	{
		return sp.getBoundingBox();
	}

	public void setBoundingBox(AABB boundingBox)
	{
		sp.setBoundingBox(boundingBox);
	}

	// 1:1 math port; only the MC accessor names changed.
	protected void moveFlying(float moveUpward, float moveStrafing, float moveForward, float speedFactor, boolean treeDimensional)
	{
		float diffMotionXStrafing = 0, diffMotionXForward = 0, diffMotionZStrafing = 0, diffMotionZForward = 0;
		{
			float total = Mth.sqrt(moveStrafing * moveStrafing + moveForward * moveForward);
			if(total >= 0.01F)
			{
				if(total < 1.0F)
					total = 1.0F;

				float moveStrafingFactor = moveStrafing / total;
				float moveForwardFactor = moveForward / total;
				float sin = Mth.sin((sp.getYRot() * 3.141593F) / 180F);
				float cos = Mth.cos((sp.getYRot() * 3.141593F) / 180F);
				diffMotionXStrafing = moveStrafingFactor * cos;
				diffMotionXForward = -moveForwardFactor * sin;
				diffMotionZStrafing = moveStrafingFactor * sin;
				diffMotionZForward = moveForwardFactor * cos;
			}
		}

		float rotation = treeDimensional ? sp.getXRot() / RadiantToAngle : 0;
		float divingHorizontalFactor = Mth.cos(rotation);
		float divingVerticalFactor = -Mth.sin(rotation) * Math.signum(moveForward);

		float diffMotionX = diffMotionXForward * divingHorizontalFactor + diffMotionXStrafing;
		float diffMotionY = Mth.sqrt(diffMotionXForward * diffMotionXForward + diffMotionZForward * diffMotionZForward) * divingVerticalFactor + moveUpward;
		float diffMotionZ = diffMotionZForward * divingHorizontalFactor + diffMotionZStrafing;

		float total = Mth.sqrt(Mth.sqrt(diffMotionX * diffMotionX + diffMotionZ * diffMotionZ) + diffMotionY * diffMotionY);
		if(total > 0.01F)
		{
			float factor = speedFactor / total;
			Vec3 m = sp.getDeltaMovement();
			sp.setDeltaMovement(m.x + diffMotionX * factor, m.y + diffMotionY * factor, m.z + diffMotionZ * factor);
		}
	}

	// --- climb / ladder / vine queries ---

	protected Block supportsCeilingClimbing(int i, int j, int k)
	{
		BlockState state = getState(i, j, k);
		if(state == null)
			return null;

		Block block = state.getBlock();

		if(block == Blocks.IRON_BARS)
			return block;

		if(block instanceof TrapDoorBlock && !getValue(state, TrapDoorBlock.OPEN))
			return block;

		return null;
	}

	protected boolean isLava(Block block)
	{
		if(block == Blocks.LAVA)
			return true;
		return block != null && block.defaultBlockState().getFluidState().is(FluidTags.LAVA);
	}

	protected float getLiquidBorder(int i, int j, int k)
	{
		float finiteLiquidBorder;
		Block block = getBlock(i, j, k);
		if(block == Blocks.WATER)
			return getNormalWaterBorder(i, j, k);
		if(SmartMovingOptions.hasFiniteLiquid && (finiteLiquidBorder = getFiniteLiquidWaterBorder(i, j, k, block)) > 0)
			return finiteLiquidBorder;
		if(block == Blocks.LAVA)
			return Config._lavaLikeWater.value ? getNormalWaterBorder(i, j, k) : 0F;

		BlockState material = getMaterial(i, j, k);
		FluidState fluid = material.getFluidState();
		if(material == null || fluid.is(FluidTags.LAVA))
			return Config._lavaLikeWater.value ? 1F : 0F;
		if(fluid.is(FluidTags.WATER))
			return getNormalWaterBorder(i, j, k);
		if(!fluid.isEmpty())
			return 1F;

		return 0F;
	}

	protected float getNormalWaterBorder(int i, int j, int k)
	{
		int level = getValue(getState(i, j, k), LiquidBlock.LEVEL);
		if(level >= 8)
			return 1F;
		if(level == 0)
			if(getState(i, j + 1, k).isAir())
				return 0.8875F;
			else
				return 1F;
		return (8 - level) / 8F;
	}

	protected float getFiniteLiquidWaterBorder(int i, int j, int k, Block block)
	{
		int type;
		if((type = Orientation.getFiniteLiquidWater(block)) > 0)
		{
			if(type == 2)
				return 1F;
			if(type == 1)
			{
				Block aboveBlock = getBlock(i, j + 1, k);
				if(Orientation.getFiniteLiquidWater(aboveBlock) > 0)
					return 1F;
				return getValue(getState(i, j, k), LiquidBlock.LEVEL) / 16F;
			}
		}
		return 0F;
	}

	public boolean isFacedToLadder(boolean isSmall)
	{
		return getOnLadder(1, true, isSmall) > 0;
	}

	public boolean isFacedToSolidVine(boolean isSmall)
	{
		return getOnVine(1, true, isSmall) > 0;
	}

	public boolean isOnLadderOrVine(boolean isSmall)
	{
		return getOnLadderOrVine(1, false, isSmall) > 0;
	}

	public boolean isOnVine(boolean isSmall)
	{
		return getOnLadderOrVine(1, false, false, true, isSmall) > 0;
	}

	public boolean isOnLadder(boolean isSmall)
	{
		return getOnLadderOrVine(1, false, true, false, isSmall) > 0;
	}

	protected int getOnLadder(int maxResult, boolean faceOnly, boolean isSmall)
	{
		return getOnLadderOrVine(maxResult, faceOnly, true, false, isSmall);
	}

	protected int getOnVine(int maxResult, boolean faceOnly, boolean isSmall)
	{
		return getOnLadderOrVine(maxResult, faceOnly, false, true, isSmall);
	}

	protected int getOnLadderOrVine(int maxResult, boolean faceOnly, boolean isSmall)
	{
		return getOnLadderOrVine(maxResult, faceOnly, true, true, isSmall);
	}

	protected int getOnLadderOrVine(int maxResult, boolean faceOnly, boolean ladder, boolean vine, boolean isSmall)
	{
		int i = Mth.floor(sp.getX());
		int minj = Mth.floor(getBoundingBox().minY);
		int k = Mth.floor(sp.getZ());

		if(Config.isStandardBaseClimb())
		{
			Block block = getBlock(i, minj, k);
			if(ladder)
				if(vine)
					return Orientation.isClimbable(sp.level(), i, minj, k) ? 1 : 0;
				else
					return block != Blocks.VINE && Orientation.isClimbable(sp.level(), i, minj, k) ? 1 : 0;
			else
				if(vine)
					return block == Blocks.VINE && Orientation.isClimbable(sp.level(), i, minj, k) ? 1 : 0;
				else
					return 0;
		}
		else
		{
			if(isSmall)
				minj--;

			HashSet<Orientation> facedOnlyTo = null;
			if(faceOnly)
				facedOnlyTo = Orientation.getClimbingOrientations(sp, true, false);

			int result = 0;
			int maxj = Mth.floor(getBoundingBox().minY + Math.ceil(getBoundingBox().maxY - getBoundingBox().minY)) - 1;
			for(int j = minj; j <= maxj; j++)
			{
				BlockState state = getState(i, j, k);
				if(ladder)
				{
					boolean localLadder = Orientation.isKnownLadder(state);
					Orientation localLadderOrientation = null;
					if(localLadder)
					{
						localLadderOrientation = Orientation.getKnownLadderOrientation(sp.level(), i, j, k);
						if(facedOnlyTo == null || facedOnlyTo.contains(localLadderOrientation))
							result++;
					}

					for(Orientation direction : facedOnlyTo != null ? facedOnlyTo : Orientation.Orthogonals)
					{
						if(result >= maxResult)
							return result;

						if(direction != localLadderOrientation)
						{
							BlockState remoteState = getState(i + direction._i, j, k + direction._k);
							if(Orientation.isKnownLadder(remoteState))
							{
								Orientation remoteLadderOrientation = Orientation.getKnownLadderOrientation(sp.level(), i + direction._i, j, k + direction._k);
								if(remoteLadderOrientation.rotate(180) == direction)
									result++;
							}
						}
					}
				}

				if(result >= maxResult)
					return result;

				if(vine && Orientation.isVine(state))
					if(facedOnlyTo == null)
						result++;
					else
					{
						Iterator<Orientation> iterator = facedOnlyTo.iterator();
						while(iterator.hasNext())
						{
							Orientation climbOrientation = iterator.next();
							if(climbOrientation.hasVineOrientation(sp.level(), i, j, k) && climbOrientation.isRemoteSolid(sp.level(), i, j, k))
							{
								result++;
								break;
							}
						}
					}

				if(result >= maxResult)
					return result;
			}
			return result;
		}
	}

	public boolean climbingUpIsBlockedByLadder()
	{
		if(sp.horizontalCollision && sp.verticalCollision && !sp.onGround() && esp.input.forwardImpulse > 0F)
		{
			Orientation orientation = Orientation.getOrientation(sp, 20F, true, false);
			if(orientation != null)
			{
				int i = Mth.floor(sp.getX());
				int j = Mth.floor(getBoundingBox().maxY);
				int k = Mth.floor(sp.getZ());
				if(Orientation.isLadder(getState(i, j, k)))
					return Orientation.getKnownLadderOrientation(sp.level(), i, j, k) == orientation;
			}
		}
		return false;
	}

	public boolean climbingUpIsBlockedByTrapDoor()
	{
		if(sp.horizontalCollision && sp.verticalCollision && !sp.onGround() && esp.input.forwardImpulse > 0F)
		{
			Orientation orientation = Orientation.getOrientation(sp, 20F, true, false);
			if(orientation != null)
			{
				int i = Mth.floor(sp.getX());
				int j = Mth.floor(getBoundingBox().maxY);
				int k = Mth.floor(sp.getZ());
				if(Orientation.isTrapDoor(getState(i, j, k)))
					return Orientation.getOpenTrapDoorOrientation(sp.level(), i, j, k) == orientation;
			}
		}
		return false;
	}

	public boolean climbingUpIsBlockedByCobbleStoneWall()
	{
		if(sp.horizontalCollision && sp.verticalCollision && !sp.onGround() && esp.input.forwardImpulse > 0F)
		{
			Orientation orientation = Orientation.getOrientation(sp, 20F, true, false);
			if(orientation != null)
			{
				int i = Mth.floor(sp.getX());
				int j = Mth.floor(getBoundingBox().maxY);
				int k = Mth.floor(sp.getZ());
				if(getBlock(i, j, k) == Blocks.COBBLESTONE_WALL)
					return !wallCanConnectTo(new BlockPos(i - orientation._i, j, k - orientation._k));
			}
		}
		return false;
	}

	// 1.8.9 BlockWall.canConnectTo(IBlockAccess, BlockPos) replica
	private boolean wallCanConnectTo(BlockPos pos)
	{
		BlockState state = sp.level().getBlockState(pos);
		Block block = state.getBlock();
		if(block == Blocks.BARRIER)
			return false;
		if(block instanceof WallBlock || block instanceof FenceGateBlock)
			return true;
		return state.isRedstoneConductor(sp.level(), pos) && block != Blocks.PUMPKIN && block != Blocks.JACK_O_LANTERN;
	}

	// --- solid / liquid column queries ---

	private List<AABB> getPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
	{
		AABB bb = getBoundingBox();
		bb = new AABB(bb.minX, yMin, bb.minZ, bb.maxX, yMax, bb.maxZ);
		if(horizontalTolerance != 0)
			bb = bb.inflate(horizontalTolerance, 0, horizontalTolerance);
		List<AABB> result = new ArrayList<AABB>();
		for(VoxelShape shape : sp.level().getBlockCollisions(sp, bb))
			result.addAll(shape.toAabbs());
		return result;
	}

	protected boolean isPlayerInSolidBetween(double yMin, double yMax)
	{
		return getPlayerSolidBetween(yMin, yMax, 0).size() > 0;
	}

	protected double getMaxPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
	{
		List<AABB> solids = getPlayerSolidBetween(yMin, yMax, horizontalTolerance);
		double result = yMin;
		for(int i = 0; i < solids.size(); i++)
		{
			AABB box = solids.get(i);
			if(isCollided(box, yMin, yMax, horizontalTolerance))
				result = Math.max(result, box.maxY);
		}
		return Math.min(result, yMax);
	}

	protected double getMinPlayerSolidBetween(double yMin, double yMax, double horizontalTolerance)
	{
		List<AABB> solids = getPlayerSolidBetween(yMin, yMax, horizontalTolerance);
		double result = yMax;
		for(int i = 0; i < solids.size(); i++)
		{
			AABB box = solids.get(i);
			if(isCollided(box, yMin, yMax, horizontalTolerance))
				result = Math.min(result, box.minY);
		}
		return Math.max(result, yMin);
	}

	protected boolean isInLiquid()
	{
		return
			getMaxPlayerLiquidBetween(getBoundingBox().minY, getBoundingBox().maxY) != getBoundingBox().minY ||
			getMinPlayerLiquidBetween(getBoundingBox().minY, getBoundingBox().maxY) != getBoundingBox().maxY;
	}

	protected double getMaxPlayerLiquidBetween(double yMin, double yMax)
	{
		int i = Mth.floor(sp.getX());
		int jMin = Mth.floor(yMin);
		int jMax = Mth.floor(yMax);
		int k = Mth.floor(sp.getZ());

		for(int j = jMax; j >= jMin; j--)
		{
			float swimWaterBorder = getLiquidBorder(i, j, k);
			if(swimWaterBorder > 0)
				return j + swimWaterBorder;
		}
		return yMin;
	}

	protected double getMinPlayerLiquidBetween(double yMin, double yMax)
	{
		int i = Mth.floor(sp.getX());
		int jMin = Mth.floor(yMin);
		int jMax = Mth.floor(yMax);
		int k = Mth.floor(sp.getZ());

		for(int j = jMin; j <= jMax; j++)
		{
			float swimWaterBorder = getLiquidBorder(i, j, k);
			if(swimWaterBorder > 0)
				if(j > yMin)
					return j;
				else if(j + swimWaterBorder > yMin)
					return yMin;
		}
		return yMax;
	}

	public boolean isCollided(AABB box, double yMin, double yMax, double horizontalTolerance)
	{
		return
			box.maxX >= getBoundingBox().minX - horizontalTolerance &&
			box.minX <= getBoundingBox().maxX + horizontalTolerance &&
			box.maxY >= yMin &&
			box.minY <= yMax &&
			box.maxZ >= getBoundingBox().minZ - horizontalTolerance &&
			box.minZ <= getBoundingBox().maxZ + horizontalTolerance;
	}

	private boolean isHeadspaceFree(BlockPos pos, int height, boolean top)
	{
		for (int y = 0; y < height; y++)
			if (isOpenBlockSpace(pos.offset(0, y, 0), top))
				return false;
		return true;
	}

	protected boolean pushOutOfBlocks(double x, double y, double z, boolean top)
	{
		BlockPos blockpos = BlockPos.containing(x, y, z);
		double d3 = x - blockpos.getX();
		double d4 = z - blockpos.getZ();

		int entHeight = Math.max(Math.round(sp.getBbHeight()), 1);

		boolean inTranslucentBlock = isHeadspaceFree(blockpos, entHeight, top);
		if (inTranslucentBlock)
		{
			byte b0 = -1;
			double d5 = 9999.0D;
			if ((!isHeadspaceFree(blockpos.west(), entHeight, top)) && (d3 < d5))
			{
				d5 = d3;
				b0 = 0;
			}
			if ((!isHeadspaceFree(blockpos.east(), entHeight, top)) && (1.0D - d3 < d5))
			{
				d5 = 1.0D - d3;
				b0 = 1;
			}
			if ((!isHeadspaceFree(blockpos.north(), entHeight, top)) && (d4 < d5))
			{
				d5 = d4;
				b0 = 4;
			}
			if ((!isHeadspaceFree(blockpos.south(), entHeight, top)) && (1.0D - d4 < d5))
			{
				d5 = 1.0D - d4;
				b0 = 5;
			}

			float f = 0.1F;
			if (b0 == 0)
				sp.setDeltaMovement(-f, sp.getDeltaMovement().y, sp.getDeltaMovement().z);
			if (b0 == 1)
				sp.setDeltaMovement(f, sp.getDeltaMovement().y, sp.getDeltaMovement().z);
			if (b0 == 4)
				sp.setDeltaMovement(sp.getDeltaMovement().x, sp.getDeltaMovement().y, -f);
			if (b0 == 5)
				sp.setDeltaMovement(sp.getDeltaMovement().x, sp.getDeltaMovement().y, f);
		}
		return false;
	}

	private boolean isOpenBlockSpace(BlockPos pos, boolean top)
	{
		return !sp.level().getBlockState(pos).isRedstoneConductor(sp.level(), pos) && (!top || !sp.level().getBlockState(pos.above()).isRedstoneConductor(sp.level(), pos.above()));
	}

	// 1.8.9 isInsideOfMaterial(Material) -> fluid-tag membership in 1.20.1
	public boolean isInsideOfMaterial(TagKey<Fluid> fluidTag)
	{
		if(SmartMovingOptions.hasFiniteLiquid && fluidTag == FluidTags.WATER)
		{
			double d = sp.getY() + sp.getEyeHeight();
			int i = Mth.floor(sp.getX());
			int j = Mth.floor(d);
			int k = Mth.floor(sp.getZ());
			Block l = getBlock(i, j, k);
			float border;
			if(l != null && (border = getFiniteLiquidWaterBorder(i, j, k, l)) > 0)
			{
				float f = (1 - border) - 0.1111111F;
				float f1 = (j + 1) - f;
				return d < f1;
			} else
			{
				return false;
			}
		}
		return isp.localIsInsideOfMaterial(fluidTag);
	}

	private List<AABB> getCollidingBoxes(AABB box)
	{
		List<AABB> result = new ArrayList<>();
		for (VoxelShape shape : sp.level().getBlockCollisions(sp, box))
			result.addAll(shape.toAabbs());
		return result;
	}

	private static double calculateXOffset(AABB box, AABB other, double offsetX)
	{
		if (other.maxY > box.minY && other.minY < box.maxY && other.maxZ > box.minZ && other.minZ < box.maxZ)
		{
			if (offsetX > 0.0D && other.maxX <= box.minX)
			{
				double d1 = box.minX - other.maxX;
				if (d1 < offsetX)
					offsetX = d1;
			}
			else if (offsetX < 0.0D && other.minX >= box.maxX)
			{
				double d0 = box.maxX - other.minX;
				if (d0 > offsetX)
					offsetX = d0;
			}
			return offsetX;
		}
		else
			return offsetX;
	}

	private static double calculateYOffset(AABB box, AABB other, double offsetY)
	{
		if (other.maxX > box.minX && other.minX < box.maxX && other.maxZ > box.minZ && other.minZ < box.maxZ)
		{
			if (offsetY > 0.0D && other.maxY <= box.minY)
			{
				double d1 = box.minY - other.maxY;
				if (d1 < offsetY)
					offsetY = d1;
			}
			else if (offsetY < 0.0D && other.minY >= box.maxY)
			{
				double d0 = box.maxY - other.minY;
				if (d0 > offsetY)
					offsetY = d0;
			}
			return offsetY;
		}
		else
			return offsetY;
	}

	private static double calculateZOffset(AABB box, AABB other, double offsetZ)
	{
		if (other.maxX > box.minX && other.minX < box.maxX && other.maxY > box.minY && other.minY < box.maxY)
		{
			if (offsetZ > 0.0D && other.maxZ <= box.minZ)
			{
				double d1 = box.minZ - other.maxZ;
				if (d1 < offsetZ)
					offsetZ = d1;
			}
			else if (offsetZ < 0.0D && other.minZ >= box.maxZ)
			{
				double d0 = box.maxZ - other.minZ;
				if (d0 > offsetZ)
					offsetZ = d0;
			}
			return offsetZ;
		}
		else
			return offsetZ;
	}

	public int calculateSeparateCollisions(double x, double y, double z)
	{
		boolean isInWeb = isp.getIsInWebField();
		AABB boundingBox = getBoundingBox();
		boolean onGround = sp.onGround();
		float stepHeight = sp.maxUpStep();

		if (isInWeb)
		{
			isInWeb = false;
			x *= 0.25D;
			y *= 0.0500000007450581D;
			z *= 0.25D;
		}
		double d6 = x;
		double d7 = y;
		double d8 = z;
		boolean flag = onGround && isSneaking();
		if (flag)
		{
			double d9 = 0.05D;
			for (; (x != 0.0D) && (getCollidingBoxes(boundingBox.move(x, -1.0D, 0.0D)).isEmpty()); d6 = x)
			{
				if ((x < d9) && (x >= -d9))
					x = 0.0D;
				else if (x > 0.0D)
					x -= d9;
				else
					x += d9;
			}
			for (; (z != 0.0D) && (getCollidingBoxes(boundingBox.move(0.0D, -1.0D, z)).isEmpty()); d8 = z)
			{
				if ((z < d9) && (z >= -d9))
					z = 0.0D;
				else if (z > 0.0D)
					z -= d9;
				else
					z += d9;
			}
			for (; (x != 0.0D) && (z != 0.0D) && (getCollidingBoxes(boundingBox.move(x, -1.0D, z)).isEmpty()); d8 = z)
			{
				if ((x < d9) && (x >= -d9))
					x = 0.0D;
				else if (x > 0.0D)
					x -= d9;
				else
					x += d9;
				d6 = x;
				if ((z < d9) && (z >= -d9))
					z = 0.0D;
				else if (z > 0.0D)
					z -= d9;
				else
					z += d9;
			}
		}
		List<AABB> list1 = getCollidingBoxes(boundingBox.expandTowards(x, y, z));
		AABB axisalignedbb = boundingBox;
		for (AABB aabb : list1)
			y = calculateYOffset(aabb, boundingBox, y);
		boundingBox = boundingBox.move(0.0D, y, 0.0D);
		boolean flag1 = onGround || ((d7 != y) && (d7 < 0.0D));
		for (AABB aabb : list1)
			x = calculateXOffset(aabb, boundingBox, x);
		boundingBox = boundingBox.move(x, 0.0D, 0.0D);
		for (AABB aabb : list1)
			z = calculateZOffset(aabb, boundingBox, z);
		boundingBox = boundingBox.move(0.0D, 0.0D, z);
		if ((stepHeight > 0.0F) && (flag1) && ((d6 != x) || (d8 != z)))
		{
			double d14 = x;
			double d10 = y;
			double d11 = z;
			AABB axisalignedbb3 = boundingBox;
			boundingBox = axisalignedbb;
			y = stepHeight;
			List<AABB> list = getCollidingBoxes(boundingBox.expandTowards(d6, y, d8));
			AABB axisalignedbb4 = boundingBox;
			AABB axisalignedbb5 = axisalignedbb4.expandTowards(d6, 0.0D, d8);
			double d12 = y;
			for (AABB aabb : list)
				d12 = calculateYOffset(aabb, axisalignedbb5, d12);
			axisalignedbb4 = axisalignedbb4.move(0.0D, d12, 0.0D);
			double d18 = d6;
			for (AABB aabb : list)
				d18 = calculateXOffset(aabb, axisalignedbb4, d18);
			axisalignedbb4 = axisalignedbb4.move(d18, 0.0D, 0.0D);
			double d19 = d8;
			for (AABB aabb : list)
				d19 = calculateZOffset(aabb, axisalignedbb4, d19);
			axisalignedbb4 = axisalignedbb4.move(0.0D, 0.0D, d19);
			AABB axisalignedbb13 = boundingBox;
			double d20 = y;
			for (AABB aabb : list)
				d20 = calculateYOffset(aabb, axisalignedbb13, d20);
			axisalignedbb13 = axisalignedbb13.move(0.0D, d20, 0.0D);
			double d21 = d6;
			for (AABB aabb : list)
				d21 = calculateXOffset(aabb, axisalignedbb13, d21);
			axisalignedbb13 = axisalignedbb13.move(d21, 0.0D, 0.0D);
			double d22 = d8;
			for (AABB aabb : list)
				d22 = calculateZOffset(aabb, axisalignedbb13, d22);
			axisalignedbb13 = axisalignedbb13.move(0.0D, 0.0D, d22);
			double d23 = d18 * d18 + d19 * d19;
			double d13 = d21 * d21 + d22 * d22;
			if (d23 > d13)
			{
				x = d18;
				z = d19;
				boundingBox = axisalignedbb4;
			}
			else
			{
				x = d21;
				z = d22;
				boundingBox = axisalignedbb13;
			}
			y = -stepHeight;
			for (AABB aabb : list)
				y = calculateYOffset(aabb, boundingBox, y);
			boundingBox = boundingBox.move(0.0D, y, 0.0D);
			if (d14 * d14 + d11 * d11 >= x * x + z * z)
			{
				x = d14;
				y = d10;
				z = d11;
				boundingBox = axisalignedbb3;
			}
		}

		boolean isCollidedPositiveX = d6 > x;
		boolean isCollidedNegativeX = d6 < x;
		boolean isCollidedPositiveY = d7 > y;
		boolean isCollidedNegativeY = d7 < y;
		boolean isCollidedPositiveZ = d8 > z;
		boolean isCollidedNegativeZ = d8 < z;

		int result = 0;
		if(isCollidedPositiveX)
			result += CollidedPositiveX;
		if(isCollidedNegativeX)
			result += CollidedNegativeX;
		if(isCollidedPositiveY)
			result += CollidedPositiveY;
		if(isCollidedNegativeY)
			result += CollidedNegativeY;
		if(isCollidedPositiveZ)
			result += CollidedPositiveZ;
		if(isCollidedNegativeZ)
			result += CollidedNegativeZ;
		return result;
	}

	public final static int CollidedPositiveX = 1;
	public final static int CollidedNegativeX = 2;
	public final static int CollidedPositiveY = 4;
	public final static int CollidedNegativeY = 8;
	public final static int CollidedPositiveZ = 16;
	public final static int CollidedNegativeZ = 32;

	public boolean isSneaking()
	{
		return sp.isShiftKeyDown();
	}

	public void correctOnUpdate(boolean isSmall, boolean reverseMaterialAcceleration)
	{
		double d = sp.getX() - sp.xo;
		double d1 = sp.getZ() - sp.zo;
		float f = (float)Math.sqrt(d * d + d1 * d1);
		if(f < 0.05F && f > 0.02 && isSmall)
		{
			float f1 = sp.yBodyRot;

			f1 = ((float)Math.atan2(d1, d) * 180F) / 3.141593F - 90F;

			if(sp.attackAnim > 0.0F)
			{
				f1 = sp.getYRot();
			}
			float f4;
			for(f4 = f1 - sp.yBodyRot; f4 < -180F; f4 += 360F) { }
			for(; f4 >= 180F; f4 -= 360F) { }
			float x = sp.yBodyRot + f4 * 0.3F;
			float f5;
			for(f5 = sp.getYRot() - x; f5 < -180F; f5 += 360F) { }
			for(; f5 >= 180F; f5 -= 360F) { }
			if(f5 < -75F)
			{
				f5 = -75F;
			}
			if(f5 >= 75F)
			{
				f5 = 75F;
			}
			sp.yBodyRot = sp.getYRot() - f5;
			if(f5 * f5 > 2500F)
			{
				sp.yBodyRot += f5 * 0.2F;
			}
			for(; sp.yBodyRot - sp.yBodyRotO < -180F; sp.yBodyRotO -= 360F) { }
			for(; sp.yBodyRot - sp.yBodyRotO >= 180F; sp.yBodyRotO += 360F) { }
		}

		if(reverseMaterialAcceleration)
			reverseHandleMaterialAcceleration();
	}

	protected double getGapUnderneight()
	{
		return getBoundingBox().minY - getMaxPlayerSolidBetween(getBoundingBox().minY - 1.1D, getBoundingBox().minY, 0);
	}

	protected double getGapOverneight()
	{
		return getMinPlayerSolidBetween(getBoundingBox().maxY, getBoundingBox().maxY + 1.1D, 0) - getBoundingBox().maxY;
	}

	public double getOverGroundHeight(double maximum)
	{
		if(esp != null)
			return (getBoundingBox().minY - getMaxPlayerSolidBetween(getBoundingBox().minY - maximum, getBoundingBox().minY, 0));
		return (getBoundingBox().minY + 1D - getMaxPlayerSolidBetween(getBoundingBox().minY - maximum + 1D, getBoundingBox().minY + 1D, 0.1));
	}

	public BlockState getOverGroundBlockId(double distance)
	{
		int x = Mth.floor(sp.getX());
		int y = Mth.floor(getBoundingBox().minY);
		int z = Mth.floor(sp.getZ());
		int minY = y - (int)Math.ceil(distance);

		if(esp == null)
		{
			y++;
			minY++;
		}

		for(; y >= minY; y--)
		{
			BlockState state = getState(x, y, z);
			if(state != null)
				return state;
		}
		return null;
	}

	public void reverseHandleMaterialAcceleration()
	{
		AABB axisalignedbb = getBoundingBox().inflate(0.0D, -0.40000000596046448D, 0.0D).deflate(0.001D);

		int i = Mth.floor(axisalignedbb.minX);
		int j = Mth.floor(axisalignedbb.maxX + 1.0D);
		int k = Mth.floor(axisalignedbb.minY);
		int l = Mth.floor(axisalignedbb.maxY + 1.0D);
		int i1 = Mth.floor(axisalignedbb.minZ);
		int j1 = Mth.floor(axisalignedbb.maxZ + 1.0D);
		if (!sp.level().hasChunksAt(i, k, i1, j, l, j1))
			return;

		Vec3 vec3 = Vec3.ZERO;
		for (int k1 = i; k1 < j; k1++)
			for (int l1 = k; l1 < l; l1++)
				for (int i2 = i1; i2 < j1; i2++)
				{
					BlockPos blockpos = new BlockPos(k1, l1, i2);
					FluidState fluidstate = sp.level().getFluidState(blockpos);
					if (fluidstate.is(FluidTags.WATER))
					{
						double d0 = l1 + fluidstate.getHeight(sp.level(), blockpos);
						if (l >= d0)
						{
							vec3 = vec3.add(fluidstate.getFlow(sp.level(), blockpos));
						}
					}
				}

		if ((vec3.length() > 0.0D) && (sp.isPushedByFluid()))
		{
			vec3 = vec3.normalize();
			double d1 = -0.014D; // instead +0.014D for reversal
			sp.setDeltaMovement(sp.getDeltaMovement().add(vec3.x * d1, vec3.y * d1, vec3.z * d1));
		}
	}
}
