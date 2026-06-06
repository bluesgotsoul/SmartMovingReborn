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

import java.util.*;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import net.smart.moving.config.*;
import net.smart.moving.render.SmartMovingRender;

public class SmartMovingClient extends SmartMovingContext implements ISmartMovingClient
{
	private final Map<String, Float> maximumExhaustionValues = new HashMap<String, Float>();
	private boolean nativeUserInterfaceDrawing = true;

	@Override
	public float getMaximumExhaustion()
	{
		float maxExhaustion = Config.getMaxExhaustion();
		if(maximumExhaustionValues.size() > 0)
		{
			Iterator<Float> iterator = maximumExhaustionValues.values().iterator();
			while(iterator.hasNext())
				maxExhaustion = Math.max(iterator.next(), maxExhaustion);
		}
		return maxExhaustion;
	}

	@Override
	public float getMaximumUpJumpCharge()
	{
		return Config._jumpChargeMaximum.value;
	}

	@Override
	public float getMaximumHeadJumpCharge()
	{
		return Config._headJumpChargeMaximum.value;
	}

	@Override
	public void setMaximumExhaustionValue(String key, float value)
	{
		maximumExhaustionValues.put(key, value);
	}

	@Override
	public float getMaximumExhaustionValue(String key)
	{
		return maximumExhaustionValues.get(key);
	}

	@Override
	public boolean removeMaximumExhaustionValue(String key)
	{
		return maximumExhaustionValues.remove(key) != null;
	}

	@Override
	public void setNativeUserInterfaceDrawing(boolean value)
	{
		nativeUserInterfaceDrawing = value;
	}

	@Override
	public boolean getNativeUserInterfaceDrawing()
	{
		return nativeUserInterfaceDrawing;
	}

	// ==================================================================
	// 1.20.1 client bootstrap.
	// In 1.8.9 this wiring lived in the mod's @SideOnly(CLIENT) proxy paths;
	// in Forge 1.20.1 it is registered on the mod event bus. SmartMovingMod
	// invokes SmartMovingClient.init(modEventBus) via DistExecutor on the client.
	// ==================================================================

	public static void init(IEventBus modEventBus)
	{
		modEventBus.addListener(SmartMovingClient::onClientSetup);
		modEventBus.addListener(SmartMovingClient::onRegisterGuiOverlays);
		modEventBus.addListener(SmartMovingClient::onRegisterKeyMappings);
		// NOTE[input-layer]: registered via RegisterKeyMappingsEvent (1.8.9: ClientRegistry.registerKeyBinding).
		// NOTE[render-layer]: EntityRenderersEvent.AddLayers is NOT needed here.
		// In 1.8.9 SmartRenderContext.registerRenderers(RenderPlayer.class) and the
		// SmartMovingMod equivalent replaced the RenderPlayer in RenderManager.
		// In 1.20.1 PlayerRendererMixin is applied to all PlayerRenderer instances at
		// class-load time, so no renderer swap or AddLayers handler is required.

		// 1.8.9 PlayerAPI overrode getFovModifier() to route through
		// moving.getFOVMultiplier(); the faithful 1.20.1 equivalent is the
		// client-only ComputeFovModifierEvent on the main Forge event bus.
		MinecraftForge.EVENT_BUS.addListener(SmartMovingClient::onComputeFovModifier);

		// 1.8.9 SmartMovingInstall patched GuiNewChat to scan incoming chat messages
		// for color-code server config packets (SmartMovingComm.processBlockCode).
		// 1.20.1 equivalent: ClientChatReceivedEvent fires for every chat message
		// before it is displayed; no reflection or chatLines field access needed.
		MinecraftForge.EVENT_BUS.addListener(SmartMovingClient::onClientChatReceived);
		
		// In 1.8.9 tick event was routed from SmartMovingMod to SmartMovingContext.
		MinecraftForge.EVENT_BUS.addListener(SmartMovingClient::onClientTick);
	}

	private static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event)
	{
		if (event.phase == net.minecraftforge.event.TickEvent.Phase.START)
		{
			SmartMovingContext.onTickInGame();
		}
	}

	private static void onClientSetup(final FMLClientSetupEvent event)
	{
		// Equivalent of the 1.8.9 client-side initialize() call.
		event.enqueueWork(() -> {
			net.smart.moving.SmartMovingFactory.initialize();
			SmartMovingContext.initialize();
		});
		SmartMovingMod.LOGGER.info("[Smart Moving] client setup complete; awaiting movement + render layers.");
	}

	// 1.8.9 -> 1.20.1 PORT NOTE (in-game GUI bars):
	//   1.8.9 drew Smart Moving's exhaustion / jump-charge bars from SmartMovingSelf.beforeGetSleepTimer,
	//   piggy-backing on the vanilla per-frame GuiIngame call to Entity.getSleepTimer(). In Forge 1.20.1
	//   the faithful, supported equivalent is a registered HUD overlay; the draw code in
	//   SmartMovingRender.renderGuiIngame is unchanged. It is layered above the vanilla HUD, matching the
	//   old "drawn after the standard in-game GUI" behaviour.
	private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event)
	{
		event.registerAboveAll("smartmoving_status", (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
			SmartMovingRender.renderGuiIngame(guiGraphics, Minecraft.getInstance()));
	}

	private static void onClientChatReceived(ClientChatReceivedEvent event)
	{
		// 1.8.9: SmartMovingInstall coremod patched GuiNewChat to call
		// SmartMovingComm.processBlockCode on each incoming chat message.
		// processBlockCode checks for the special §0§1...§f§f pattern that
		// a Smart Moving server uses to push config blocks via chat.
		SmartMovingComm.processBlockCode(event.getMessage().getString());
	}

	private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
	{
		// 1.8.9 ClientRegistry.registerKeyBinding -> 1.20.1 RegisterKeyMappingsEvent.register
		// Registers the four SmartMoving keybinds from SmartMovingOptions (SmartMovingContext.Options).
		net.smart.moving.config.SmartMovingOptions opts = SmartMovingContext.Options;
		event.register(opts.keyBindGrab);
		event.register(opts.keyBindConfigToggle);
		event.register(opts.keyBindSpeedIncrease);
		event.register(opts.keyBindSpeedDecrease);
	}

	private static void onComputeFovModifier(ComputeFovModifierEvent event)
	{
		Player player = event.getPlayer();
		if(!(player instanceof LocalPlayer) || !(player instanceof IEntityPlayerSP))
			return;

		IEntityPlayerSP sp = (IEntityPlayerSP)player;
		if(sp.isInFovQuery())
			return;

		SmartMoving moving = sp.getMoving();
		if(moving instanceof SmartMovingSelf)
			event.setNewFovModifier(((SmartMovingSelf)moving).getFOVMultiplier());
	}
}
