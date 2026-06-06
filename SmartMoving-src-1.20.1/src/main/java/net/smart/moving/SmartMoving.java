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
// Abstract orchestrator shared by SmartMovingSelf (client) and
// SmartMovingOther (other players). Holds the public movement-state flags that
// are serialized into the state packet and consumed by the render layer.

package net.smart.moving;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public abstract class SmartMoving extends SmartMovingBase
{
	public boolean isSlow;
	public boolean isFast;

	public boolean isClimbing;
	public boolean isHandsVineClimbing;
	public boolean isFeetVineClimbing;

	public boolean isClimbJumping;
	public boolean isClimbBackJumping;
	public boolean isWallJumping;
	public boolean isClimbCrawling;
	public boolean isCrawlClimbing;
	public boolean isCeilingClimbing;
	public boolean isRopeSliding;

	public boolean isDipping;
	public boolean isSwimming;
	public boolean isDiving;
	public boolean isLevitating;
	public boolean isHeadJumping;
	public boolean isCrawling;
	public boolean isSliding;
	public boolean isFlying;

	public int actualHandsClimbType;
	public int actualFeetClimbType;

	public int angleJumpType;

	public float heightOffset;

	private float spawnSlindingParticle;
	private float spawnSwimmingParticle;

	public SmartMoving(Player sp, IEntityPlayerSP isp)
	{
		super(sp, isp);
	}

	public boolean isAngleJumping()
	{
		return angleJumpType > 1 && angleJumpType < 7;
	}

	public abstract boolean isJumping();

	public abstract boolean doFlyingAnimation();

	public abstract boolean doFallingAnimation();

	protected void spawnParticles(Minecraft minecraft, double playerMotionX, double playerMotionZ)
	{
		float horizontalSpeedSquare = 0;
		if(isSliding || isSwimming)
			horizontalSpeedSquare = (float)(playerMotionX * playerMotionX + playerMotionZ * playerMotionZ);

		if(isSliding)
		{
			int i = Mth.floor(sp.getX());
			int j = Mth.floor(sp.getBoundingBox().minY - 0.1F);
			int k = Mth.floor(sp.getZ());
			Block block = getBlock(i, j, k);
			if(block != null)
			{
				double posY = sp.getBoundingBox().minY + 0.1D;
				double motionX = -playerMotionX * 4D;
				double motionY = 1.5D;
				double motionZ = -playerMotionZ * 4D;

				spawnSlindingParticle += horizontalSpeedSquare;

				float maxSpawnSlindingParticle = Config._slideParticlePeriodFactor.value * 0.1F;
				while(spawnSlindingParticle > maxSpawnSlindingParticle)
				{
					double posX = sp.getX() + getSpawnOffset();
					double posZ = sp.getZ() + getSpawnOffset();
					sp.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, getState(i, j, k)), posX, posY, posZ, motionX, motionY, motionZ);
					spawnSlindingParticle -= maxSpawnSlindingParticle;
				}
			}
		}

		if(isSwimming)
		{
			float posY = Mth.floor(sp.getBoundingBox().minY) + 1.0F;
			int i = (int)Math.floor(sp.getX());
			int j = (int)Math.floor(posY - 0.5);
			int k = (int)Math.floor(sp.getZ());

			Block block = getBlock(i, j, k);

			boolean isLava = block != null && isLava(block);
			spawnSwimmingParticle += horizontalSpeedSquare;

			float maxSpawnSwimmingParticle = (isLava ? Config._lavaSwimParticlePeriodFactor.value : Config._swimParticlePeriodFactor.value) * 0.01F;
			while(spawnSwimmingParticle > maxSpawnSwimmingParticle)
			{
				double posX = sp.getX() + getSpawnOffset();
				double posZ = sp.getZ() + getSpawnOffset();
				sp.level().addParticle(isLava ? ParticleTypes.LAVA : ParticleTypes.SPLASH, posX, posY, posZ, 0D, 0.2D, 0D);
				spawnSwimmingParticle -= maxSpawnSwimmingParticle;
			}
		}
	}

	private float getSpawnOffset()
	{
		return (sp.getRandom().nextFloat() - 0.5F) * 2F * sp.getBbWidth();
	}

	protected void onStartClimbBackJump()
	{
		// 1.8.9 1:1: nudge the player's previous render yaw so the climb-back jump spins the model.
		// getPreviousRendererData takes an AbstractClientPlayer; sp is the client player here (matching the
		// original, where these jump hooks only run client-side), so cast as the port does elsewhere.
		net.smart.render.SmartRenderRender.getPreviousRendererData((LocalPlayer)sp).rotateAngleY += isHeadJumping ? Half : Quarter;
		isClimbBackJumping = true;
	}

	protected void onStartWallJump(Float angle)
	{
		// 1.8.9 1:1: set the player's previous render yaw to the wall-jump angle so the model faces away.
		if (angle != null)
			net.smart.render.SmartRenderRender.getPreviousRendererData((LocalPlayer)sp).rotateAngleY = angle / RadiantToAngle;
		isWallJumping = true;
		sp.fallDistance = 0F;
	}
}
