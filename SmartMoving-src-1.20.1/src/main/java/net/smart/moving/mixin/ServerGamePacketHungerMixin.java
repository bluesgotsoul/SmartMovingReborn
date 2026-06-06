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
// 1.20.1 port of the coremod transformation
//   net.smart.core.SmartCoreEventHandler.NetHandlerPlayServer_before/afterProcessPlayer
// dispatched by net.smart.moving.SmartMovingCoreEventHandler:
//   beforeProcessPlayer -> moving.beforeAddMovingHungerBatch()
//   afterProcessPlayer  -> moving.afterAddMovingHungerBatch()
//
// The 1.8.9 coremod wrapped the server movement-packet handler
// NetHandlerPlayServer.processPlayer(C03PacketPlayer) so the server-side
// exhaustion produced while applying the moved position is batched (the depth
// counter in SmartMovingServer makes this nest safely with the PlayerAPI
// updatePotionEffects batch reproduced in LivingEntityMixin -- a separate,
// distinct wrap). The 1.20.1 equivalent method is
//   ServerGamePacketListenerImpl.handleMovePlayer(ServerboundMovePlayerPacket).
// The handler owns its player in the public 'player' field (shadowed); the
// SmartMovingServer is reached through (IEntityPlayerMP).getMoving() exactly as
// the original dispatched through SmartMovingServerPlayerBase.moving. The class
// exists on both the integrated (client) and dedicated server, so this is a
// common mixin.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketHungerMixin
{
	@Shadow public ServerPlayer player;

	@Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V", at = @At("HEAD"))
	private void smartmoving$beforeProcessPlayer(CallbackInfo ci)
	{
		SmartMovingServer moving = ((IEntityPlayerMP)player).getMoving();
		if(moving != null)
			moving.beforeAddMovingHungerBatch();
	}

	@Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V", at = @At("RETURN"))
	private void smartmoving$afterProcessPlayer(CallbackInfo ci)
	{
		SmartMovingServer moving = ((IEntityPlayerMP)player).getMoving();
		if(moving != null)
			moving.afterAddMovingHungerBatch();
	}
}
