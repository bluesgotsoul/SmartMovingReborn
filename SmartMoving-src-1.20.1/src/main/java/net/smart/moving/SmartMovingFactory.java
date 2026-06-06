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
// 1.20.1 PORT. Client-side registry of SmartMovingOther instances keyed by
// remote-player entity id. 1:1 with 1.8.9.
// EntityOtherPlayerMP -> RemotePlayer; theWorld -> level; getEntityID -> getId.

package net.smart.moving;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SmartMovingFactory extends SmartMovingContext
{
	private static SmartMovingFactory factory;

	private Hashtable<Integer, SmartMovingOther> otherSmartMovings;

	public SmartMovingFactory()
	{
		if(factory != null)
			throw new RuntimeException("FATAL: Can only create one instance of type 'SmartMovingFactory'");
		factory = this;
	}

	protected static boolean isInitialized()
	{
		return factory != null;
	}

	public static void initialize()
	{
		if(!isInitialized())
			new SmartMovingFactory();
	}

	public static void handleMultiPlayerTick(Minecraft minecraft)
	{
		factory.doHandleMultiPlayerTick(minecraft);
	}

	public static SmartMoving getInstance(Player entityPlayer)
	{
		return factory.doGetInstance(entityPlayer);
	}

	public static SmartMoving getOtherSmartMoving(int entityId)
	{
		return factory.doGetOtherSmartMoving(entityId);
	}

	public static SmartMovingOther getOtherSmartMoving(RemotePlayer entity)
	{
		return factory.doGetOtherSmartMoving(entity);
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
				SmartMovingOther moving = doGetOtherSmartMoving(otherPlayer);
				moving.spawnParticles(minecraft, otherPlayer.getX() - otherPlayer.xo, otherPlayer.getZ() - otherPlayer.zo);
				moving.foundAlive = true;
			}
		}

		if(otherSmartMovings == null || otherSmartMovings.isEmpty())
			return;

		Iterator<Integer> entityIds = otherSmartMovings.keySet().iterator();
		while(entityIds.hasNext())
		{
			Integer entityId = entityIds.next();
			SmartMovingOther moving = otherSmartMovings.get(entityId);
			if(moving.foundAlive)
				moving.foundAlive = false;
			else
				entityIds.remove();
		}
	}

	protected SmartMoving doGetInstance(Player entityPlayer)
	{
		if(entityPlayer instanceof RemotePlayer)
			return doGetOtherSmartMoving(entityPlayer.getId());
		else if(entityPlayer instanceof IEntityPlayerSP)
			return ((IEntityPlayerSP)entityPlayer).getMoving();
		return null;
	}

	protected SmartMoving doGetOtherSmartMoving(int entityId)
	{
		SmartMoving moving = tryGetOtherSmartMoving(entityId);
		if(moving == null)
		{
			Entity entity = Minecraft.getInstance().level.getEntity(entityId);
			if(entity != null && entity instanceof RemotePlayer)
				moving = addOtherSmartMoving((RemotePlayer)entity);
		}
		return moving;
	}

	protected SmartMovingOther doGetOtherSmartMoving(RemotePlayer entity)
	{
		SmartMovingOther moving = tryGetOtherSmartMoving(entity.getId());
		if(moving == null)
			moving = addOtherSmartMoving(entity);
		return moving;
	}

	protected final SmartMovingOther tryGetOtherSmartMoving(int entityId)
	{
		if(otherSmartMovings == null)
			otherSmartMovings = new Hashtable<Integer, SmartMovingOther>();
		return otherSmartMovings.get(entityId);
	}

	protected final SmartMovingOther addOtherSmartMoving(RemotePlayer entity)
	{
		SmartMovingOther moving = new SmartMovingOther(entity);
		otherSmartMovings.put(entity.getId(), moving);
		return moving;
	}
}
