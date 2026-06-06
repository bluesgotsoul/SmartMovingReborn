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

// 1.8.9 -> 1.20.1: receiver callbacks carry NetworkEvent.Context instead of the raw
// FMLProxyPacket (see IPacketReceiver). The original bodies never read the packet object,
// so the behaviour is unchanged.
import net.minecraftforge.network.NetworkEvent;

public class SmartMovingServerComm implements IPacketReceiver
{
	public static ILocalUserNameProvider localUserNameProvider = null;

	@Override
	public boolean processStatePacket(NetworkEvent.Context context, IEntityPlayerMP player, int entityId, long state)
	{
		player.getMoving().processStatePacket(entityId, state);
		return true;
	}

	@Override
	public boolean processConfigInfoPacket(NetworkEvent.Context context, IEntityPlayerMP player, String info)
	{
		player.getMoving().processConfigPacket(info);
		return true;
	}

	@Override
	public boolean processConfigContentPacket(NetworkEvent.Context context, IEntityPlayerMP player, String[] content, String username)
	{
		return false;
	}

	@Override
	public boolean processConfigChangePacket(NetworkEvent.Context context, IEntityPlayerMP player)
	{
		player.getMoving().processConfigChangePacket(localUserNameProvider != null ? localUserNameProvider.getLocalConfigUserName() : null);
		return true;
	}

	@Override
	public boolean processSpeedChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, int difference, String username)
	{
		player.getMoving().processSpeedChangePacket(difference, localUserNameProvider != null ? localUserNameProvider.getLocalSpeedUserName() : null);
		return true;
	}

	@Override
	public boolean processHungerChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, float hunger)
	{
		player.getMoving().processHungerChangePacket(hunger);
		return true;
	}

	@Override
	public boolean processSoundPacket(NetworkEvent.Context context, IEntityPlayerMP player, String soundId, float distance, float pitch)
	{
		player.getMoving().processSoundPacket(soundId, distance, pitch);
		return true;
	}

	public static final SmartMovingServerComm instance = new SmartMovingServerComm();
}
