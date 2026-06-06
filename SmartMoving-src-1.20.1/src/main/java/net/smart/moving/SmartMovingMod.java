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

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// 1.8.9 architecture note:
//   The original Smart Moving was a coremod (net.smart.core ASM transformer) plus an @Mod
//   that, during init, registered an FML network channel, registered tick events, the
//   PlayerAPI player-bases (SmartMovingPlayerBase / SmartMovingServerPlayerBase) and the
//   custom RenderPlayer. The deep movement behaviour lived in SmartMovingSelf, driven by
//   bytecode hooks the coremod injected into EntityPlayerSP / EntityPlayer.
//
// 1.20.1 architecture note:
//   * The coremod + PlayerAPI bytecode hooks are replaced by Mixins (net.smart.moving.mixin)
//     into LivingEntity / Player / LocalPlayer and the client input classes.
//   * The custom network channel becomes a Forge SimpleChannel (see SmartMovingNetwork).
//   * Rendering is provided by the ported Smart Render plus the Smart Moving model/render
//     classes, wired on the client only.
@Mod(SmartMovingMod.MOD_ID)
public final class SmartMovingMod
{
	public static final String MOD_ID = "smartmoving";
	public static final String MOD_COM_VERSION = "2.4";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SmartMovingMod()
	{
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		modEventBus.addListener(this::commonSetup);
		
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);

		// Client-only wiring (renderer hookup, key bindings, options) lives in SmartMovingClient.
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SmartMovingClient.init(modEventBus));
	}

	private void commonSetup(final FMLCommonSetupEvent event)
	{
		// Registers the SimpleChannel (replaces the 1.8.9 newEventDrivenChannel registration).
		SmartMovingNetwork.register();
		LOGGER.info("[Smart Moving] common setup complete (communication protocol {}).", MOD_COM_VERSION);
	}
	
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event)
	{
		net.smart.moving.SmartMovingServer.initialize(new java.io.File("."), event.getServer().getDefaultGameType().getId(), new net.smart.moving.config.SmartMovingConfig());
	}
}
