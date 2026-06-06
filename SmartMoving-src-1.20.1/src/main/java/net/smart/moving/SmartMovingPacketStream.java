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

import java.io.*;
import java.util.*;

// 1.8.9 imported net.minecraftforge.fml.common.network.internal.FMLProxyPacket here.
// In 1.20.1 the SimpleChannel handler hands us the raw byte[] payload plus a
// NetworkEvent.Context, so receivePacket takes those directly. The (de)serialization of the
// SmartMoving payload via Object{Input,Output}Stream is preserved byte-for-byte.
import net.minecraftforge.network.NetworkEvent;

public class SmartMovingPacketStream
{
	public static final String Id;
	public static final Set<StackTraceElement> errors;

	static
	{
		String id = SmartMovingInfo.ModComId;
		if(id.length() > 15)
			id = id.substring(0, 15);
		Id = id;
		errors = new HashSet<StackTraceElement>();
	}

	// 1.8.9: receivePacket(FMLProxyPacket packet, IPacketReceiver comm, IEntityPlayerMP player)
	//        read its bytes from packet.payload().array().
	// 1.20.1: SmartMovingNetwork delivers the decoded byte[] (and the NetworkEvent.Context that
	//         replaces the old packet object) directly.
	public static void receivePacket(byte[] data, IPacketReceiver comm, IEntityPlayerMP player, NetworkEvent.Context context)
	{
		try
		{
			ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			byte packetId = objectInput.readByte();
			switch(packetId)
			{
				case SmartMovingInfo.StatePacketId:
					int entityId = objectInput.readInt();
					long state = objectInput.readLong();
					comm.processStatePacket(context, player, entityId, state);
					break;
				case SmartMovingInfo.ConfigInfoPacketId:
					String info = (String)objectInput.readObject();
					comm.processConfigInfoPacket(context, player, info);
					break;
				case SmartMovingInfo.ConfigContentPacketId:
					String[] content = (String[])objectInput.readObject();
					String username = (String)objectInput.readObject();
					comm.processConfigContentPacket(context, player, content, username);
					break;
				case SmartMovingInfo.ConfigChangePacketId:
					comm.processConfigChangePacket(context, player);
					break;
				case SmartMovingInfo.SpeedChangePacketId:
					int difference = objectInput.readInt();
					username = (String)objectInput.readObject();
					comm.processSpeedChangePacket(context, player, difference, username);
					break;
				case SmartMovingInfo.HungerChangePacketId:
					float hunger = objectInput.readFloat();
					comm.processHungerChangePacket(context, player, hunger);
					break;
				case SmartMovingInfo.SoundPacketId:
					String soundId = (String)objectInput.readObject();
					float volume = objectInput.readFloat();
					float pitch = objectInput.readFloat();
					comm.processSoundPacket(context, player, soundId, volume, pitch);
					break;
				default:
					throw new RuntimeException("Unknown packet id '" + packetId + "' found");
			}
		}
		catch(Throwable t)
		{
			if(errors.add(t.getStackTrace()[0]))
				t.printStackTrace();
			else
				System.err.println(t.getClass().getName() + ": " + t.getMessage());
		}
	}

	public static void sendState(IPacketSender comm, int entityId, long state)
	{
		comm.sendPacket(serializeState(entityId, state));
	}

	// Builds the exact serialized state blob that sendState transmits. The server uses this to
	// re-broadcast an incoming state packet to tracked players: 1.8.9 forwarded the original
	// FMLProxyPacket verbatim, and re-serializing the identical (entityId, state) is byte-identical.
	public static byte[] serializeState(int entityId, long state)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.StatePacketId);
			objectOutput.writeInt(entityId);
			objectOutput.writeLong(state);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		return byteOutput.toByteArray();
	}

	public static void sendConfigInfo(IPacketSender comm, String info)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigInfoPacketId);
			objectOutput.writeObject(info);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendConfigContent(IPacketSender comm, String[] content, String username)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigContentPacketId);
			objectOutput.writeObject(content);
			objectOutput.writeObject(username);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendConfigChange(IPacketSender comm)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.ConfigChangePacketId);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendSpeedChange(IPacketSender comm, int difference, String username)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.SpeedChangePacketId);
			objectOutput.writeInt(difference);
			objectOutput.writeObject(username);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendHungerChange(IPacketSender comm, float hunger)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.HungerChangePacketId);
			objectOutput.writeFloat(hunger);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}

	public static void sendSound(IPacketSender comm, String soundId, float volume, float pitch)
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try
		{
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeByte(SmartMovingInfo.SoundPacketId);
			objectOutput.writeObject(soundId);
			objectOutput.writeFloat(volume);
			objectOutput.writeFloat(pitch);
			objectOutput.flush();
		}
		catch(Throwable t)
		{
			throw new RuntimeException(t);
		}
		comm.sendPacket(byteOutput.toByteArray());
	}
}
