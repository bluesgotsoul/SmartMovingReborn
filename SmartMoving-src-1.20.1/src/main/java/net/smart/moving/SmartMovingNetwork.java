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

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

//
// 1.8.9 transport:
//   NetworkRegistry.INSTANCE.newEventDrivenChannel(SmartMovingPacketStream.Id).register(this)
//   + a single raw C17PacketCustomPayload carrying the ObjectOutputStream byte[] blob, with the
//   ServerCustomPacketEvent / ClientCustomPacketEvent handlers dispatching to
//   SmartMovingServerComm.instance / SmartMovingComm.instance.
//
// 1.20.1 transport (this class):
//   A Forge SimpleChannel with one payload message (SmartMovingPayload) that carries the
//   IDENTICAL byte[] blob produced by SmartMovingPacketStream. Only the transport envelope
//   changes (SimpleChannel adds a discriminator + a varint length prefix); the SmartMoving
//   payload bytes are byte-for-byte the same on both ends, so the on-protocol semantics are
//   preserved 1:1.
//
//   The protocol version is the original ModComVersion ("2.4"), which keeps the communication
//   compatibility gate that 1.8.9 enforced through SmartMovingInfo.ModComId / ModComVersion:
//   peers running a different Smart Moving communication protocol fail the channel handshake.
//
// NOTE: 1.8.9 used SmartMovingPacketStream.Id (the truncated ModComId, e.g. "SmartMoving 2.4")
// as the channel name. That string is not a valid 1.20.1 ResourceLocation (uppercase + space),
// so the channel id is the mod id ("smartmoving:main"); the version compatibility that the old
// id encoded is carried by PROTOCOL_VERSION instead.
//
public final class SmartMovingNetwork
{
	private static final String PROTOCOL_VERSION = SmartMovingInfo.ModComVersion;

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(SmartMovingMod.MOD_ID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals);

	private SmartMovingNetwork()
	{
	}

	// Called from SmartMovingMod#commonSetup; replaces the 1.8.9 newEventDrivenChannel registration.
	public static void register()
	{
		int index = 0;
		CHANNEL.registerMessage(
			index++,
			SmartMovingPayload.class,
			SmartMovingPayload::encode,
			SmartMovingPayload::decode,
			SmartMovingPayload::handle);
	}

	// Client -> server. 1.8.9 equivalent: SmartMovingComm.sendPacket(byte[]) sending a
	// C17PacketCustomPayload to the (integrated or remote) server.
	public static void sendToServer(byte[] data)
	{
		CHANNEL.sendToServer(new SmartMovingPayload(data));
	}

	// Server -> a single client.
	public static void sendToPlayer(byte[] data, ServerPlayer player)
	{
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SmartMovingPayload(data));
	}

	// Server -> all players tracking the entity. 1.8.9 equivalent:
	// IEntityPlayerMP.sendPacketToTrackedPlayers(FMLProxyPacket).
	public static void sendToTrackedPlayers(byte[] data, Entity entity)
	{
		CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new SmartMovingPayload(data));
	}

	public static final class SmartMovingPayload
	{
		private final byte[] data;

		public SmartMovingPayload(byte[] data)
		{
			this.data = data;
		}

		public byte[] getData()
		{
			return data;
		}

		public static void encode(SmartMovingPayload message, FriendlyByteBuf buffer)
		{
			buffer.writeByteArray(message.data);
		}

		public static SmartMovingPayload decode(FriendlyByteBuf buffer)
		{
			return new SmartMovingPayload(buffer.readByteArray());
		}

		public static void handle(SmartMovingPayload message, Supplier<NetworkEvent.Context> contextSupplier)
		{
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() ->
			{
				if(context.getDirection() == NetworkDirection.PLAY_TO_SERVER)
				{
					// 1.8.9: onPacketData(ServerCustomPacketEvent) ->
					//   receivePacket(packet, SmartMovingServerComm.instance,
					//     SmartMovingServerPlayerBase.getPlayerBase(playerEntity))
					ServerPlayer sender = context.getSender();
					// NOTE[server-layer]: getPlayerBase(sender) wraps ServerPlayer into IEntityPlayerMP
					// once the server player base (PlayerAPI -> Mixin) is ported.
					IEntityPlayerMP player = sender != null
						? (IEntityPlayerMP)sender
						: null;
					SmartMovingPacketStream.receivePacket(message.data, SmartMovingServerComm.instance, player, context);
				}
				else
				{
					// 1.8.9: onPacketData(ClientCustomPacketEvent) ->
					//   receivePacket(packet, SmartMovingComm.instance, null)
					SmartMovingPacketStream.receivePacket(message.data, SmartMovingComm.instance, null, context);
				}
			});
			context.setPacketHandled(true);
		}
	}
}
