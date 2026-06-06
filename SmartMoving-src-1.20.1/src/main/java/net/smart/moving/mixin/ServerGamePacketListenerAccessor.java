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
// Accessor for net.minecraft.server.network.ServerGamePacketListenerImpl#aboveGroundTickCount.
//
// 1.8.9 IEntityPlayerMP.resetTicksForFloatKick reflectively zeroed
// NetHandlerPlayServer.ticksForFloatKick. The 1.20.1 equivalent is the private
// aboveGroundTickCount counter that triggers the "flying is not enabled" kick.
// This accessor lets ServerPlayerMixin reset it so climbing/crawling does not
// trip the anti-fly kick. The class exists on client (integrated server) and
// dedicated server, so this is a common mixin.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor
{
	@Accessor("aboveGroundTickCount")
	void smartmoving$setAboveGroundTickCount(int aboveGroundTickCount);
}
