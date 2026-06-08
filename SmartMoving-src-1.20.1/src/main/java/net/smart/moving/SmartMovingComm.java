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

// 1.8.9 -> 1.20.1 type adaptation:
//   Minecraft.getMinecraft()            -> Minecraft.getInstance()
//   theWorld.getEntityByID(id)          -> level.getEntity(id)
//   getNetHandler().addToSendQueue(..)  -> SmartMovingNetwork.sendToServer(byte[])
//   MinecraftServer.getServer()         -> ServerLifecycleHooks.getCurrentServer()
//   getIntegratedServer()/isSinglePlayer -> getSingleplayerServer()/isSingleplayer()
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.smart.moving.config.*;
import net.smart.properties.*;

public class SmartMovingComm extends SmartMovingContext implements IPacketReceiver, IPacketSender {
	@Override
	public boolean processStatePacket(NetworkEvent.Context context, IEntityPlayerMP player, int entityId, long state) {
		Entity entity = Minecraft.getInstance().level.getEntity(entityId);
		if (entity == null)
			return true;

		// 1.8.9 -> 1.20.1 PORT NOTE:
		// Apply the received remote-player movement state to that player's
		// SmartMovingOther so its
		// Smart Moving animation (climb / swim / crawl / slide / ...) is visible to
		// everyone, not just
		// to the player themselves. EntityOtherPlayerMP -> RemotePlayer. The rest of
		// the multiplayer
		// pipeline (transport SmartMovingNetwork, server rebroadcast
		// SmartMovingServerComm, the
		// SmartMovingFactory registry, the per-tick handleMultiPlayerTick, and the
		// renderer reading
		// SmartMovingFactory.getInstance) is already wired -- this client receiver was
		// the only missing
		// link; the original 1.8.9 body was left commented out in the port.
		if (entity instanceof RemotePlayer) {
			SmartMovingOther moving = SmartMovingFactory.getOtherSmartMoving((RemotePlayer) entity);
			if (moving != null)
				moving.processStatePacket(state);
		}
		return true;
	}

	@Override
	public boolean processConfigInfoPacket(NetworkEvent.Context context, IEntityPlayerMP player, String info) {
		return false;
	}

	@Override
	public boolean processConfigContentPacket(NetworkEvent.Context context, IEntityPlayerMP player, String[] content,
			String username) {
		processConfigPacket(content, username, false);
		return true;
	}

	@Override
	public boolean processConfigChangePacket(NetworkEvent.Context context, IEntityPlayerMP player) {
		SmartMovingOptions.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer());
		return true;
	}

	@Override
	public boolean processSpeedChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, int difference,
			String username) {
		if (difference == 0)
			SmartMovingOptions.writeNoRightsToChangeSpeedMessageToChat(isConnectedToRemoteServer());
		else {
			Config.changeSpeed(difference);
			Options.writeServerSpeedMessageToChat(username, Config._globalConfig.value);
		}
		return true;
	}

	@Override
	public boolean processHungerChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, float hunger) {
		return false;
	}

	@Override
	public boolean processSoundPacket(NetworkEvent.Context context, IEntityPlayerMP player, String soundId,
			float distance, float pitch) {
		return false;
	}

	private static boolean isConnectedToRemoteServer() {
		return ServerLifecycleHooks.getCurrentServer() == null
				|| Minecraft.getInstance().getSingleplayerServer() == null
				|| !Minecraft.getInstance().getSingleplayerServer().isSingleplayer();
	}

	public static void processConfigPacket(String[] content, String username, boolean blockCode) {
		boolean isGloballyConfigured = false;
		if (content != null && content.length == 2 && Options._globalConfig.getCurrentKey().equals(content[0])) {
			isGloballyConfigured = "true".equals(content[1]);
			content = null;
		}

		boolean wasEnabled = Config.enabled;
		boolean first = Config != ServerConfig;
		if (first)
			ServerConfig.reset();

		if (content != null)
			if (content.length != 0) {
				ServerConfig.loadFromProperties(content, blockCode);
				isGloballyConfigured = ServerConfig._globalConfig.value;
			} else {
				Config = Options;
				Options.writeServerDeconfigMessageToChat();
				return;
			}
		else {
			ServerConfig.load(false);
			ServerConfig.setCurrentKey(null);
		}

		ServerConfig._globalConfig.value = isGloballyConfigured;

		if (!first) {
			Options.writeServerReconfigMessageToChat(wasEnabled, username, isGloballyConfigured);
			return;
		}

		Config = ServerConfig;
		Options.writeServerConfigMessageToChat();
		if (!blockCode)
			SmartMovingPacketStream.sendConfigInfo(SmartMovingComm.instance, SmartMovingConfig._sm_current);
	}

	@Override
	public void sendPacket(byte[] data) {
		SmartMovingNetwork.sendToServer(data);
	}

	public static boolean processBlockCode(String text) {
		if (!text.startsWith("\u00A70\u00A71") || !text.endsWith("\u00A7f\u00A7f"))
			return false;

		String codes = text.substring(4, text.length() - 4);
		processBlockCode(codes, "\u00A70", Options._baseClimb, "standard");
		processBlockCode(codes, "\u00A71", Options._freeClimb);
		processBlockCode(codes, "\u00A72", Options._ceilingClimbing);
		processBlockCode(codes, "\u00A73", Options._swim);
		processBlockCode(codes, "\u00A74", Options._dive);
		processBlockCode(codes, "\u00A75", Options._crawl);
		processBlockCode(codes, "\u00A76", Options._slide);
		processBlockCode(codes, "\u00A77", Options._fly);
		processBlockCode(codes, "\u00A78", Options._jumpCharge);
		processBlockCode(codes, "\u00A79", Options._headJump);
		processBlockCode(codes, "\u00A7a", Options._angleJumpSide);
		processBlockCode(codes, "\u00A7b", Options._angleJumpBack);
		return true;
	}

	private static void processBlockCode(String text, String blockCode, Property<?> property, String... value) {
		if (text.contains(blockCode))
			processConfigPacket(new String[] { property.getCurrentKey(), value.length > 0 ? value[0] : "false" }, null,
					true);
	}

	public static final SmartMovingComm instance = new SmartMovingComm();
}
