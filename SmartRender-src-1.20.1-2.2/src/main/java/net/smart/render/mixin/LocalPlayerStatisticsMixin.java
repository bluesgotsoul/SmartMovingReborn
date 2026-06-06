// ==================================================================
// This file is part of Smart Render.
//
// Smart Render is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Smart Render is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Smart Render. If not, see <http://www.gnu.org/licenses/>.
// ==================================================================

package net.smart.render.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.smart.render.statistics.IEntityPlayerSP;
import net.smart.render.statistics.SmartStatistics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   The original "net.smart.render.statistics.playerapi" package hooked the local player through
//   PlayerAPI (ClientPlayerBase: afterMoveEntityWithHeading -> calculateAllStats(false),
//   afterUpdateRidden -> calculateRiddenStats) and registered an IEntityPlayerSP back-end.
//   PlayerAPI does not exist for 1.20.1, so this Mixin reproduces it 1:1 by implementing
//   IEntityPlayerSP directly on LocalPlayer (so SmartStatisticsFactory.doGetInstance finds it
//   through the existing "instanceof IEntityPlayerSP" branch) and injecting the same two hooks:
//     - aiStep TAIL   == afterMoveEntityWithHeading (per-tick movement)
//     - rideTick TAIL == afterUpdateRidden
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerStatisticsMixin implements IEntityPlayerSP
{
	@Unique
	private SmartStatistics smartrender$statistics;

	@Override
	public SmartStatistics getStatistics()
	{
		if(smartrender$statistics == null)
			smartrender$statistics = new SmartStatistics((Player)(Object)this);
		return smartrender$statistics;
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void smartrender$afterMoveEntityWithHeading(CallbackInfo ci)
	{
		getStatistics().calculateAllStats(false);
	}

	@Inject(method = "rideTick", at = @At("TAIL"))
	private void smartrender$afterUpdateRidden(CallbackInfo ci)
	{
		getStatistics().calculateRiddenStats();
	}
}
