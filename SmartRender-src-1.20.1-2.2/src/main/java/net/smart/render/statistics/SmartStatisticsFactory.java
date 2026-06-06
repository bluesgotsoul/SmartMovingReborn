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

package net.smart.render.statistics;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SmartStatisticsFactory
{
	private static SmartStatisticsFactory factory;

	private Hashtable<Integer, SmartStatisticsOther> otherStatistics;

	public SmartStatisticsFactory()
	{
		if(factory != null)
			throw new RuntimeException("FATAL: Can only create one instance of type 'StatisticsFactory'");
		factory = this;
	}

	protected static boolean isInitialized()
	{
		return factory != null;
	}

	public static void initialize()
	{
		if(!isInitialized())
			new SmartStatisticsFactory();
	}

	public static void handleMultiPlayerTick(Minecraft minecraft)
	{
		factory.doHandleMultiPlayerTick(minecraft);
	}

	public static SmartStatistics getInstance(Player entityPlayer)
	{
		return factory.doGetInstance(entityPlayer);
	}

	public static SmartStatisticsOther getOtherStatistics(int entityId)
	{
		return factory.doGetOtherStatistics(entityId);
	}

	public static SmartStatisticsOther getOtherStatistics(RemotePlayer entity)
	{
		return factory.doGetOtherStatistics(entity);
	}

	protected void doHandleMultiPlayerTick(Minecraft minecraft)
	{
		Iterator<?> others = minecraft.level.players().iterator();
		while(others.hasNext())
		{
			Entity player = (Entity)others.next();
			if(player instanceof RemotePlayer)
			{
				RemotePlayer otherPlayer = (RemotePlayer)player;
				SmartStatisticsOther statistics = doGetOtherStatistics(otherPlayer);
				statistics.calculateAllStats(true);
				statistics.foundAlive = true;
			}
		}

		if(otherStatistics == null || otherStatistics.isEmpty())
			return;

		Iterator<Integer> entityIds = otherStatistics.keySet().iterator();
		while(entityIds.hasNext())
		{
			Integer entityId = entityIds.next();
			SmartStatisticsOther statistics = otherStatistics.get(entityId);
			if(statistics.foundAlive)
				statistics.foundAlive = false;
			else
				entityIds.remove();
		}
	}

	protected SmartStatistics doGetInstance(Player entityPlayer)
	{
		if(entityPlayer instanceof RemotePlayer)
			return doGetOtherStatistics(entityPlayer.getId());
		else if(entityPlayer instanceof IEntityPlayerSP)
			return ((IEntityPlayerSP)entityPlayer).getStatistics();
		return null;
	}

	protected SmartStatisticsOther doGetOtherStatistics(int entityId)
	{
		SmartStatisticsOther statistics = tryGetOtherStatistics(entityId);
		if(statistics == null)
		{
			Entity entity = Minecraft.getInstance().level.getEntity(entityId);
			if(entity != null && entity instanceof RemotePlayer)
				statistics = addOtherStatistics((RemotePlayer)entity);
		}
		return statistics;
	}

	protected SmartStatisticsOther doGetOtherStatistics(RemotePlayer entity)
	{
		SmartStatisticsOther statistics = tryGetOtherStatistics(entity.getId());
		if(statistics == null)
			statistics = addOtherStatistics(entity);
		return statistics;
	}

	protected final SmartStatisticsOther tryGetOtherStatistics(int entityId)
	{
		if(otherStatistics == null)
			otherStatistics = new Hashtable<Integer, SmartStatisticsOther>();
		return otherStatistics.get(entityId);
	}

	protected final SmartStatisticsOther addOtherStatistics(RemotePlayer entity)
	{
		SmartStatisticsOther statistics = new SmartStatisticsOther(entity);
		otherStatistics.put(entity.getId(), statistics);
		return statistics;
	}
}
