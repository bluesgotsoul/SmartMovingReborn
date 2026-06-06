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
//   net.smart.core.SmartCoreEventHandler.ItemInWorldManager_before/afterActivateBlockOrUseItem
// dispatched by net.smart.moving.SmartMovingCoreEventHandler:
//   beforeActivateBlockOrUseItem -> moving.beforeActivateBlockOrUseItem()
//   afterActivateBlockOrUseItem  -> moving.afterActivateBlockOrUseItem()
//
// The 1.8.9 coremod wrapped the server block right-click handler
//   ItemInWorldManager.activateBlockOrUseItem(EntityPlayer, World, ItemStack,
//                                              BlockPos, EnumFacing, f, f, f).
// SmartMovingServer.beforeActivateBlockOrUseItem pins forceIsSneaking to the
// real sneak state for the duration of the activation so the server's
// secondary-use check is not skewed by Smart Moving's overridden isSneaking(),
// then clears it. The 1.20.1 equivalent is
//   ServerPlayerGameMode.useItemOn(ServerPlayer, Level, ItemStack,
//                                   InteractionHand, BlockHitResult)
// (ItemInWorldManager -> ServerPlayerGameMode). The acting player arrives as the
// first argument; its SmartMovingServer is reached through
// (IEntityPlayerMP).getMoving(), exactly as the original dispatched through
// SmartMovingServerPlayerBase.moving. ServerPlayerGameMode exists on both the
// integrated (client) and dedicated server, so this is a common mixin.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingServer;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeUseMixin
{
	@Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"))
	private void smartmoving$beforeActivateBlockOrUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir)
	{
		SmartMovingServer moving = ((IEntityPlayerMP)player).getMoving();
		if(moving != null)
			moving.beforeActivateBlockOrUseItem();
	}

	@Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
	private void smartmoving$afterActivateBlockOrUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir)
	{
		SmartMovingServer moving = ((IEntityPlayerMP)player).getMoving();
		if(moving != null)
			moving.afterActivateBlockOrUseItem();
	}
}
