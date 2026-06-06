// ==================================================================
// This file is part of Smart Render.
//
// Smart Render is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Render is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Render. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================

package net.smart.render;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.smart.render.statistics.SmartStatisticsContext;
import net.smart.render.statistics.SmartStatisticsFactory;

// Client-side entry point for Smart Render.
//
// 1.8.9 -> 1.20.1 mapping of responsibilities:
//   * 1.8.9 replaced net.minecraft.client.renderer.entity.RenderPlayer with its own
//     subclass and swapped the instances inside RenderManager (see the old
//     SmartRenderFactory). It also relied on PlayerAPI hooks (ModelPlayerAPI) to drive
//     the per-bone rotation model (ModelRotationRenderer) plus the cape / ears / special
//     renderers.
//   * 1.20.1 has a data-driven model system. The equivalent hooks are:
//       - EntityRenderersEvent.AddLayers  -> attach / replace player render layers
//       - A Mixin on net.minecraft.client.model.PlayerModel / HumanoidModel to expose the
//         per-part rotation pipeline that ModelRotationRenderer used to provide.
//     These are wired in the dedicated render-porting layer; this class is the single
//     place where that wiring is registered on the mod event bus.
public final class SmartRenderClient
{
	private SmartRenderClient()
	{
	}

	public static void init(IEventBus modEventBus)
	{
		modEventBus.addListener(SmartRenderClient::onClientSetup);
		// NOTE[render-layer]: EntityRenderersEvent.AddLayers is NOT needed here.
		// 1.8.9 SmartRenderFactory replaced the RenderPlayer instances in RenderManager.
		// In 1.20.1 PlayerRendererMixin (in lib + SmartMoving) is applied at class-load
		// time to all PlayerRenderer instances, so no renderer swap is required.

		// 1.8.9: SmartRenderMod registered itself on MinecraftForge.EVENT_BUS during
		// FMLInitializationEvent and handled ClientTickEvent to drive remote-player statistics.
		// In 1.20.1 game (non-setup) events still live on the Forge event bus, so the per-tick
		// hook is registered here.
		MinecraftForge.EVENT_BUS.addListener(SmartRenderClient::onClientTick);
	}

	private static void onClientSetup(final FMLClientSetupEvent event)
	{
		// 1.8.9 created the statistics factory singleton in FMLInitializationEvent
		// (SmartStatisticsFactory.initialize()). The PlayerAPI registration that accompanied it
		// (SmartStatistics.register()) is folded into LocalPlayerStatisticsMixin, which supplies
		// IEntityPlayerSP.getStatistics() for the local player.
		event.enqueueWork(SmartStatisticsFactory::initialize);

		SmartRenderMod.LOGGER.info("[Smart Render] client setup complete; player render pipeline ready for Smart Moving hooks.");
	}

	// 1.8.9 SmartRenderMod.tickStart(ClientTickEvent): this handler had NO phase check, so it ran
	// on both TickEvent.Phase.START and END. The behaviour is preserved 1:1 here (no phase filter)
	// so remote-player statistics tick exactly as in the original.
	private static void onClientTick(final TickEvent.ClientTickEvent event)
	{
		SmartStatisticsContext.onTickInGame();
	}
}
