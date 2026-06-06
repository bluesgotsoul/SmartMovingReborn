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

// 1.8.9 passed the raw transport packet (FMLProxyPacket) to every receiver callback.
// In 1.20.1 the SimpleChannel handler exposes NetworkEvent.Context instead, which carries
// the sender/side information. The original bodies never read the packet object, so this is
// a faithful type swap (FMLProxyPacket -> NetworkEvent.Context).
import net.minecraftforge.network.NetworkEvent;

public interface IPacketReceiver
{
	boolean processStatePacket(NetworkEvent.Context context, IEntityPlayerMP player, int entityId, long state);

	boolean processConfigInfoPacket(NetworkEvent.Context context, IEntityPlayerMP player, String info);

	boolean processConfigContentPacket(NetworkEvent.Context context, IEntityPlayerMP player, String[] content, String username);

	boolean processConfigChangePacket(NetworkEvent.Context context, IEntityPlayerMP player);

	boolean processSpeedChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, int difference, String username);

	boolean processHungerChangePacket(NetworkEvent.Context context, IEntityPlayerMP player, float hunger);

	boolean processSoundPacket(NetworkEvent.Context context, IEntityPlayerMP player, String soundId, float distance, float pitch);
}
