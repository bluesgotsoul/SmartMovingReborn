// ==================================================================
// This file is part of Smart Moving.
//
// Smart Moving is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
// ==================================================================
//
// 1.20.1 port of the server-side half of the PlayerAPI base
// net.smart.moving.playerapi.SmartMovingServerPlayerBase.
//
// PlayerAPI no longer exists, so the ServerPlayer now directly implements
// IEntityPlayerMP via this mixin and owns its SmartMovingServer instance
// ("moving"). The before/after lifecycle hooks that PlayerAPI used to dispatch
// (onUpdate / onLivingUpdate / setPosition / potion-effects ...) are recreated
// as @Inject points; the ones that live on shared superclasses are injected by
// the base-class mixins (EntityMixin / LivingEntityMixin / PlayerMixin) which
// dispatch here through (IEntityPlayerMP).getMoving().
//
// 1.8.9 -> 1.20.1 mappings used here:
//   player.height (mutable field) -> mirrored in smartmoving$height (no 1.20.1 setter)
//   getEntityBoundingBox()        -> getBoundingBox()
//   setEntityBoundingBox(AABB)    -> setBoundingBox(AABB)
//   box.expand(x,y,z)             -> box.inflate(x,y,z)
//   worldObj.getEntitiesWithinAABBExcludingEntity -> level().getEntities(self, box)
//   entity.isDead                 -> !entity.isAlive()
//   entity.onCollideWithPlayer(p) -> entity.playerTouch(p)
//   super.addExhaustion(f)        -> getFoodData().addExhaustion(f)  (no re-entry)
//   player.playSound(id,v,p)      -> BuiltInRegistries.SOUND_EVENT.get + Entity.playSound
//   FMLProxyPacket transport      -> SmartMovingNetwork raw byte[] dispatch

package net.smart.moving.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.smart.moving.IEntityPlayerMP;
import net.smart.moving.SmartMovingNetwork;
import net.smart.moving.SmartMovingServer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements IEntityPlayerMP
{
	@Unique private SmartMovingServer smartmoving$moving;

	// 1.8.9 mirrored the mutable Entity.height field; 1.20.1 has no setter, so
	// setHeight stores it here and getHeight returns it (-1 = not yet set, falls
	// back to the live bounding-box height).
	@Unique private float smartmoving$height = -1F;

	@Unique
	private ServerPlayer smartmoving$self()
	{
		return (ServerPlayer)(Object)this;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void smartmoving$init(CallbackInfo ci)
	{
		smartmoving$moving = new SmartMovingServer(this, false);
	}

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void smartmoving$beforeTick(CallbackInfo ci)
	{
		if(smartmoving$moving != null)
			smartmoving$moving.beforeOnUpdate();
	}

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void smartmoving$afterTick(CallbackInfo ci)
	{
		if(smartmoving$moving != null)
			smartmoving$moving.afterOnUpdate();
	}

	// ------------------------------------------------------------------
	// IEntityPlayerMP
	// ------------------------------------------------------------------

	@Override
	public SmartMovingServer getMoving()
	{
		return smartmoving$moving;
	}

	@Override
	public float getHeight()
	{
		// 1.8.9 returned the mutable Entity.height field that setHeight wrote. 1.20.1
		// derives height from Pose/EntityDimensions, so we mirror the field; before the
		// first setHeight we fall back to the live bounding-box height.
		return smartmoving$height >= 0F ? smartmoving$height : smartmoving$self().getBbHeight();
	}

	@Override
	public double getMinY()
	{
		return smartmoving$self().getBoundingBox().minY;
	}

	@Override
	public void setMaxY(double maxY)
	{
		ServerPlayer p = smartmoving$self();
		AABB box = p.getBoundingBox();
		p.setBoundingBox(new AABB(box.minX, box.minY, box.minZ, box.maxX, maxY, box.maxZ));
	}

	@Override
	public float doGetHealth()
	{
		return smartmoving$self().getHealth();
	}

	@Override
	public AABB getBox()
	{
		return smartmoving$self().getBoundingBox();
	}

	@Override
	public AABB expandBox(AABB box, double x, double y, double z)
	{
		return box.inflate(x, y, z);
	}

	@Override
	public List<?> getEntitiesExcludingPlayer(AABB box)
	{
		ServerPlayer p = smartmoving$self();
		return p.level().getEntities(p, box);
	}

	@Override
	public boolean isDeadEntity(Entity entity)
	{
		return !entity.isAlive();
	}

	@Override
	public void onCollideWithPlayer(Entity entity)
	{
		entity.playerTouch(smartmoving$self());
	}

	@Override
	public String getUsername()
	{
		return smartmoving$self().getGameProfile().getName();
	}

	@Override
	public void resetFallDistance()
	{
		ServerPlayer p = smartmoving$self();
		p.fallDistance = 0;
		Vec3 m = p.getDeltaMovement();
		p.setDeltaMovement(m.x, 0.08, m.z);
	}

	@Override
	public void sendPacket(byte[] data)
	{
		SmartMovingNetwork.sendToPlayer(data, smartmoving$self());
	}

	@Override
	public void sendPacketToTrackedPlayers(byte[] data)
	{
		SmartMovingNetwork.sendToTrackedPlayers(data, smartmoving$self());
	}

	@Override
	public IEntityPlayerMP[] getAllPlayers()
	{
		ServerPlayer p = smartmoving$self();
		List<ServerPlayer> players = p.server.getPlayerList().getPlayers();
		IEntityPlayerMP[] result = new IEntityPlayerMP[players.size()];
		for(int i = 0; i < players.size(); i++)
			result[i] = (IEntityPlayerMP)(Object)players.get(i);
		return result;
	}

	@Override
	public boolean localIsSneaking()
	{
		return smartmoving$self().isShiftKeyDown();
	}

	@Override
	public void localAddExhaustion(float exhaustion)
	{
		// 1.8.9 super.addExhaustion -> FoodStats.addExhaustion; route straight to the
		// vanilla food data so the Smart Moving exhaustion hook is not re-entered.
		smartmoving$self().getFoodData().addExhaustion(exhaustion);
	}

	@Override
	public void setHeight(float height)
	{
		// 1.8.9 player.height = height. The authoritative server collision box is set
		// separately via setMaxY; here we record the height that getHeight() must return
		// so the setMaxY box math matches the original 1:1.
		smartmoving$height = height;
	}

	@Override
	public void resetTicksForFloatKick()
	{
		// 1.8.9 reflectively zeroed NetHandlerPlayServer.ticksForFloatKick; the 1.20.1
		// equivalent is ServerGamePacketListenerImpl.aboveGroundTickCount, reset here so
		// climbing/crawling does not trip the "flying is not enabled" kick.
		ServerGamePacketListenerImpl connection = smartmoving$self().connection;
		if(connection != null)
			((ServerGamePacketListenerAccessor)connection).smartmoving$setAboveGroundTickCount(0);
	}

	@Override
	public boolean localIsEntityInsideOpaqueBlock()
	{
		// 1.8.9 super.isEntityInsideOpaqueBlock() -> 1.20.1 Entity.isInWall().
		return smartmoving$self().isInWall();
	}

	@Override
	public void localAddMovementStat(double x, double y, double z)
	{
		// 1.8.9 super.addMovementStat(x,y,z) -> 1.20.1 Player.checkMovementStatistics.
		((PlayerInvoker)(Object)smartmoving$self()).smartmoving$checkMovementStatistics(x, y, z);
	}

	@Override
	public void localPlaySound(String soundId, float volume, float pitch)
	{
		// 1.8.9 player.playSound(soundId, volume, pitch). Resolve the registry name the
		// same way as the client side and play through the entity (server broadcast).
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(soundId));
		if(sound != null)
			smartmoving$self().playSound(sound, volume, pitch);
	}
}
