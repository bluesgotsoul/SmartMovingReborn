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

package net.smart.moving;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.smart.moving.config.*;
import net.smart.render.SmartRenderContext;

public abstract class SmartMovingContext extends SmartRenderContext
{
	public static final float ClimbPullMotion = 0.3F;

	public static final double FastUpMotion = 0.2D;
	public static final double MediumUpMotion = 0.14D;
	public static final double SlowUpMotion = 0.1D;
	public static final double HoldMotion = 0.08D;
	public static final double SinkDownMotion = 0.05D;
	public static final double ClimbDownMotion = 0.01D;
	public static final double CatchCrawlGapMotion = 0.17D;

	public static final float SwimCrawlWaterMaxBorder = 1F;
	public static final float SwimCrawlWaterTopBorder = 0.65F;
	public static final float SwimCrawlWaterMediumBorder = 0.6F;
	public static final float SwimCrawlWaterBottomBorder = 0.55F;

	public static final float HorizontalGroundDamping = 0.546F;
	public static final float HorizontalAirDamping = 0.91F;
	public static final float HorizontalAirodynamicDamping = 0.999F;

	public static final float SwimSoundDistance = 1F / 0.7F;
	public static final float SlideToHeadJumpingFallDistance = 0.05F;


	public static final SmartMovingClient Client = new SmartMovingClient();
	public static final SmartMovingOptions Options = new SmartMovingOptions();
	public static final SmartMovingServerConfig ServerConfig = new SmartMovingServerConfig();
	public static SmartMovingClientConfig Config = Options;


	private static boolean wasInitialized;

	public static void onTickInGame()
	{
		Minecraft minecraft = Minecraft.getInstance();

		if(minecraft.level != null && minecraft.level.isClientSide())
		{
			// NOTE[movement-layer]: SmartMovingFactory belongs to the physics/movement
			// layer, which is not ported yet. Original 1.8.9 call:
			//   SmartMovingFactory.handleMultiPlayerTick(minecraft);
			SmartMovingFactory.handleMultiPlayerTick(minecraft);
		}

		Options.initializeForGameIfNeccessary();

		initializeServerIfNecessary();
	}

	public static void initialize()
	{
		if(!wasInitialized)
			net.smart.render.statistics.SmartStatisticsContext.setCalculateHorizontalStats(true);

		// NOTE[input-layer]: In Forge 1.20.1 key mappings are registered via
		// RegisterKeyMappingsEvent on the mod event bus (see SmartMovingClient bootstrap),
		// not the removed ClientRegistry.registerKeyBinding(...). The KeyMapping instances
		// (Options.keyBindGrab/keyBindConfigToggle/keyBindSpeedIncrease/keyBindSpeedDecrease)
		// must be created and registered there. Original 1.8.9 calls, preserved for reference:
		//   ClientRegistry.registerKeyBinding(Options.keyBindGrab);
		//   ClientRegistry.registerKeyBinding(Options.keyBindConfigToggle);
		//   ClientRegistry.registerKeyBinding(Options.keyBindSpeedIncrease);
		//   ClientRegistry.registerKeyBinding(Options.keyBindSpeedDecrease);

		if(wasInitialized)
			return;

		wasInitialized = true;

		System.out.println(SmartMovingInfo.ModComMessage);
		SmartMovingMod.LOGGER.info(SmartMovingInfo.ModComMessage);
	}

	public static void initializeServerIfNecessary()
	{
		MinecraftServer currentMinecraftServer = ServerLifecycleHooks.getCurrentServer();
		if(currentMinecraftServer != null && currentMinecraftServer != lastMinecraftServer)
		{
			GameType gameType;
			try
			{
				gameType = currentMinecraftServer.getDefaultGameType();
			}
			catch(Throwable t)
			{
				return;
			}
			SmartMovingServer.initialize(SmartMovingOptions.optionsPath, gameType.getId(), Options);
		}
		lastMinecraftServer = currentMinecraftServer;
	}

	// ==================================================================
	// Block-access primitives shared by the climbing / physics layer
	// (Orientation etc.). Faithful 1.20.1 forms of the original 1.8.9
	// static helpers.
	//
	// 1.8.9 -> 1.20.1 notes:
	//   - getState(...) used Block.getActualState(...) in 1.8.9; that API was
	//     removed and the BlockState returned by Level.getBlockState(...) is
	//     already fully resolved, so no extra resolution step is needed.
	//   - getValue(...) is a single generic accessor that covers Boolean,
	//     Integer, Direction and Enum properties (Property<T>).
	//   - The net.minecraft.block.material.Material type was REMOVED in 1.20;
	//     solidity checks are reimplemented by the movement layer via
	//     BlockState predicates (BlockState.isSolid()/blocksMotion()).
	// ==================================================================

	public static BlockState getState(Level world, int i, int j, int k)
	{
		return world.getBlockState(new BlockPos(i, j, k));
	}

	public static Block getBlock(Level world, int i, int j, int k)
	{
		return getState(world, i, j, k).getBlock();
	}

	public static <T extends Comparable<T>> T getValue(BlockState state, Property<T> property)
	{
		return state.getValue(property);
	}

	private static MinecraftServer lastMinecraftServer = null;
}
