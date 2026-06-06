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
//   net.smart.core.SmartCoreEventHandler.PlayerControllerMP_before/afterOnPlayerRightClick
// dispatched by net.smart.moving.SmartMovingCoreEventHandler:
//   beforeOnPlayerRightClick -> moving.beforeActivateBlockOrUseItem()
//   afterOnPlayerRightClick  -> moving.afterActivateBlockOrUseItem()
//
// The 1.8.9 coremod wrapped the client block right-click handler
//   PlayerControllerMP.onPlayerRightClick(EntityPlayer, World, ItemStack,
//                                          BlockPos, EnumFacing, Vec3).
// SmartMovingSelf.beforeActivateBlockOrUseItem pins forceIsSneaking to the real
// sneak button (isSneakButtonPressed) for the duration of the block activation,
// so vanilla's secondary-use check sees the physical sneak state rather than
// Smart Moving's overridden isSneaking() (crawl/climb), and clears it after.
// The 1.20.1 equivalent is the block-use path
//   MultiPlayerGameMode.useItemOn(LocalPlayer, InteractionHand, BlockHitResult)
// (the air/item path sendUseItem -> useItem was never wrapped, so it is left
// untouched -- faithful to onPlayerRightClick taking a BlockPos). The local
// player owns its client SmartMovingSelf via IEntityPlayerSP.getMoving(); guard
// instanceof SmartMovingSelf and register client-only since MultiPlayerGameMode
// and LocalPlayer are client classes.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import net.smart.moving.IEntityPlayerSP;
import net.smart.moving.SmartMoving;
import net.smart.moving.SmartMovingSelf;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeClientUseMixin
{
	@Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"))
	private void smartmoving$beforeOnPlayerRightClick(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir)
	{
		SmartMoving moving = ((IEntityPlayerSP)player).getMoving();
		if(moving instanceof SmartMovingSelf)
			((SmartMovingSelf)moving).beforeActivateBlockOrUseItem();
	}

	@Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
	private void smartmoving$afterOnPlayerRightClick(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir)
	{
		SmartMoving moving = ((IEntityPlayerSP)player).getMoving();
		if(moving instanceof SmartMovingSelf)
			((SmartMovingSelf)moving).afterActivateBlockOrUseItem();
	}
}
