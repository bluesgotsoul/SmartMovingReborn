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

package net.smart.render;

import java.util.*;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import net.smart.render.statistics.*;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   The original SmartRenderRender was driven by a RenderPlayer subclass that overrode
//   doRender / rotateCorpse / renderLayers / handleRotationFloat and called back into this
//   class around its own super.* calls. In 1.20.1 the driver is PlayerRendererMixin, which
//   @Injects the vanilla methods. To keep the math 1:1 while fitting the Mixin model, the
//   former "wrap around super" methods are split into pre/post hooks that the Mixin invokes:
//     doRender         -> doRenderPre(..) [HEAD]  + doRenderPost() [RETURN]
//     rotateCorpse     -> setupRotationsPre(..)   (returns the rotation handed to vanilla)
//     renderLayers     -> renderSpecialsBefore/After(..)  (driven from CapeLayerMixin)
//     handleRotationFloat -> before/afterHandleRotationFloat(..)  (driven around getBob)
//   All numeric logic below is copied verbatim from the 1.8.9 source.
public class SmartRenderRender extends SmartRenderContext
{
	public static SmartRenderModel CurrentMainModel;

	public IRenderPlayer irp;

	public SmartRenderRender(IRenderPlayer irp)
	{
		this.irp = irp;

		modelBipedMain = irp.createModel(irp.getModelBipedMain(), 0.0F, irp.getSmallArms()).getRenderModel();
		SmartRenderModel modelArmorChestplate = irp.createModel(irp.getModelArmorChestplate(), 1.0F, false).getRenderModel();
		SmartRenderModel modelArmor = irp.createModel(irp.getModelArmor(), 0.5F, false).getRenderModel();

		irp.initialize((PlayerModel<?>)modelBipedMain.mp, modelArmorChestplate.mp, modelArmor.mp);
	}

	// == doRender (1.8.9) split into HEAD (pre) + RETURN (post) ==
	// In 1.8.9 "isInventory" was derived from d/d1/d2/f/renderPartialTicks being zero/one. The
	// 1.20.1 PlayerRenderer.render signature no longer exposes those offsets, so PlayerRendererMixin
	// decides isInventory and passes it in. NOTE[inventory-detect]: InventoryScreen detection; verify at runtime.
	public void doRenderPre(AbstractClientPlayer entityplayer, float renderPartialTicks, boolean isInventory)
	{
		SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);
		if(statistics != null)
		{
			boolean isSleeping = entityplayer.isSleeping();

			float totalVerticalDistance = statistics.getTotalVerticalDistance(renderPartialTicks);
			float currentVerticalSpeed = statistics.getCurrentVerticalSpeed(renderPartialTicks);
			float totalDistance = statistics.getTotalDistance(renderPartialTicks);
			float currentSpeed = statistics.getCurrentSpeed(renderPartialTicks);

			double distance = 0;
			double verticalDistance = 0;
			double horizontalDistance = 0;
			float currentCameraAngle = 0;
			float currentVerticalAngle = 0;
			float currentHorizontalAngle = 0;

			if (!isInventory)
			{
				double xDiff = entityplayer.getX() - entityplayer.xo;
				double yDiff = entityplayer.getY() - entityplayer.yo;
				double zDiff = entityplayer.getZ() - entityplayer.zo;

				verticalDistance = Math.abs(yDiff);
				horizontalDistance = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
				distance = Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);

				currentCameraAngle = entityplayer.getYRot() / RadiantToAngle;
				currentVerticalAngle = (float)Math.atan(yDiff / horizontalDistance);
				if(Float.isNaN(currentVerticalAngle))
					currentVerticalAngle = Quarter;

				currentHorizontalAngle = (float)-Math.atan(xDiff / zDiff);
				if (Float.isNaN(currentHorizontalAngle))
					if(Float.isNaN(statistics.prevHorizontalAngle))
						currentHorizontalAngle = currentCameraAngle;
					else
						currentHorizontalAngle = statistics.prevHorizontalAngle;
				else if (zDiff < 0)
					currentHorizontalAngle += Half;

				statistics.prevHorizontalAngle = currentHorizontalAngle;
			}

			IModelPlayer[] modelPlayers = irp.getRenderModels();

			for(int i = 0; i < modelPlayers.length; i++)
			{
				SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();

				modelPlayer.isInventory = isInventory;

				modelPlayer.totalVerticalDistance = totalVerticalDistance;
				modelPlayer.currentVerticalSpeed = currentVerticalSpeed;
				modelPlayer.totalDistance = totalDistance;
				modelPlayer.currentSpeed = currentSpeed;

				modelPlayer.distance = distance;
				modelPlayer.verticalDistance = verticalDistance;
				modelPlayer.horizontalDistance = horizontalDistance;
				modelPlayer.currentCameraAngle = currentCameraAngle;
				modelPlayer.currentVerticalAngle = currentVerticalAngle;
				modelPlayer.currentHorizontalAngle = currentHorizontalAngle;
				modelPlayer.prevOuterRenderData = getPreviousRendererData(entityplayer);
				modelPlayer.isSleeping = isSleeping;
			}
		}

		CurrentMainModel = modelBipedMain;
	}

	public void doRenderPost()
	{
		CurrentMainModel = null;
	}

	// == rotateCorpse (1.8.9) -> setupRotations pre-hook ==
	// Returns the rotation value that should be handed to the vanilla setupRotations body
	// (0 when Smart Render takes over the body rotation, the original value otherwise).
	public float setupRotationsPre(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
	{
		boolean isLocal = entityplayer instanceof LocalPlayer;
		boolean isInventory = f2 == 1.0F && isLocal && Minecraft.getInstance().screen instanceof InventoryScreen;
		if(!isInventory)
		{
			float forwardRotation = entityplayer.yRotO + (entityplayer.getYRot() - entityplayer.yRotO) * f2;

			if(entityplayer.isSleeping())
			{
				actualRotation = 0;
				forwardRotation = 0;
			}

			float workingAngle;
			Minecraft minecraft = Minecraft.getInstance();
			if(!isLocal)
			{
				workingAngle = -entityplayer.getYRot();
				workingAngle += minecraft.getCameraEntity().getYRot();
			}
			else
				workingAngle = actualRotation - getPreviousRendererData(entityplayer).rotateAngleY * RadiantToAngle;

			if(minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT && !((Player)minecraft.getCameraEntity()).isSleeping())
				workingAngle += 180F;

			IModelPlayer[] modelPlayers = irp.getRenderModels();

			for(int i = 0; i < modelPlayers.length; i++)
			{
				SmartRenderModel modelPlayer = modelPlayers[i].getRenderModel();

				modelPlayer.actualRotation = actualRotation;
				modelPlayer.forwardRotation = forwardRotation;
				modelPlayer.workingAngle = workingAngle;
			}

			actualRotation = 0;
		}

		return actualRotation;
	}

	// == renderLayers (1.8.9 renderSpecials) -> before/after hooks (driven from CapeLayerMixin) ==
	public void renderSpecialsBefore(AbstractClientPlayer entityPlayer, float f3)
	{
		modelBipedMain.bipedEars.beforeRender(entityPlayer);
		modelBipedMain.bipedCloak.beforeRender(entityPlayer, f3);
	}

	public void renderSpecialsAfter()
	{
		modelBipedMain.bipedCloak.afterRender();
		modelBipedMain.bipedEars.afterRender();
	}

	@SuppressWarnings({ "static-method", "unused" })
	public void beforeHandleRotationFloat(AbstractClientPlayer entityPlayer, float f)
	{
		SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityPlayer);
		if (statistics != null)
			entityPlayer.tickCount += statistics.ticksRiding;
	}

	@SuppressWarnings({ "static-method", "unused" })
	public void afterHandleRotationFloat(AbstractClientPlayer entityPlayer, float f)
	{
		SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityPlayer);
		if (statistics != null)
			entityPlayer.tickCount -= statistics.ticksRiding;
	}

	public static RendererData getPreviousRendererData(AbstractClientPlayer entityplayer)
	{
		if(++previousRendererDataAccessCounter > 1000)
		{
			List<? extends Player> players = Minecraft.getInstance().level.players();

			Iterator<AbstractClientPlayer> iterator = previousRendererData.keySet().iterator();
			while(iterator.hasNext())
				if(!players.contains(iterator.next()))
					iterator.remove();

			previousRendererDataAccessCounter = 0;
		}

		RendererData result = previousRendererData.get(entityplayer);
		if(result == null)
			previousRendererData.put(entityplayer, result = new RendererData());
		return result;
	}

	private static Map<AbstractClientPlayer, RendererData> previousRendererData = new HashMap<AbstractClientPlayer, RendererData>();
	private static int previousRendererDataAccessCounter = 0;

	public final SmartRenderModel modelBipedMain;
}
