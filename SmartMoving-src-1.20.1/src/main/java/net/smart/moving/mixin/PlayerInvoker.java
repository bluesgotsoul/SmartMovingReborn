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
// Invoker for net.minecraft.world.entity.player.Player#checkMovementStatistics.
//
// 1.8.9 IEntityPlayerMP.localAddMovementStat delegated to
// super.addMovementStat(x,y,z); the 1.20.1 equivalent is the private
// Player.checkMovementStatistics(double, double, double). This invoker exposes it
// so ServerPlayerMixin can account walk/swim/dive/climb distance exactly as the
// original did. Player exists on both sides, so this is a common mixin.

package net.smart.moving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public interface PlayerInvoker
{
	@Invoker("checkMovementStatistics")
	void smartmoving$checkMovementStatistics(double dx, double dy, double dz);
}
