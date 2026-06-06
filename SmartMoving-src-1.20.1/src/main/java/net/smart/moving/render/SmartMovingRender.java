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

package net.smart.moving.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

import net.smart.moving.*;
import net.smart.render.statistics.*;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   The original SmartMovingRender was driven by a RenderPlayer subclass that overrode
//   doRender / rotateCorpse / renderLivingAt / renderName and called back into this class
//   around its own super.* calls (irp.superRender*). In 1.20.1 the driver is a Mixin on
//   PlayerRenderer, which @Injects the vanilla methods, so the former "wrap around super"
//   methods are split into pre/post hooks that the Mixin invokes (mirrors SmartRenderRender):
//     doRender      -> doRenderPre(..) [HEAD] + doRenderPost() [RETURN]
//     rotateCorpse  -> setupRotationsPre(..) [HEAD of setupRotations]
//     renderLivingAt-> renderLivingAtVerticalOffset(..) (PoseStack Y translation)
//     renderName    -> renderNamePre(..) / renderNamePost(..) around the vanilla name render
//   The two vertical render offsets that 1.8.9 applied to the d1 argument (the crawl-sneak
//   nudge in doRender and the heightOffset in renderLivingAt) are surfaced as values the Mixin
//   translates onto the PoseStack. These hooks + offsets are wired by the render Mixins:
//   net.smart.render.mixin.PlayerRendererMixin / net.smart.moving.mixin.SmartRenderRenderMixin drive
//   doRenderPre/doRenderPost/setupRotationsPre; net.smart.moving.mixin.PlayerRendererMovingMixin applies
//   the vertical offset; net.smart.moving.mixin.LivingEntityRendererNameMixin wraps the name render.
//   All numeric logic below is copied verbatim from the 1.8.9 source.
public class SmartMovingRender extends SmartRenderContext
{
	public static SmartMovingModel CurrentMainModel;

	public IRenderPlayer irp;

	public SmartMovingRender(IRenderPlayer irp)
	{
		this.irp = irp;

		modelBipedMain = irp.getMovingModelBipedMain().getMovingModel();
		SmartMovingModel modelArmorChestplate = irp.getMovingModelArmorChestplate().getMovingModel();
		SmartMovingModel modelArmor = irp.getMovingModelArmor().getMovingModel();

		modelBipedMain.scaleArmType = Scale;
		modelBipedMain.scaleLegType = Scale;
		modelArmorChestplate.scaleArmType = NoScaleStart;
		modelArmorChestplate.scaleLegType = NoScaleEnd;
		modelArmor.scaleArmType = NoScaleStart;
		modelArmor.scaleLegType = Scale;
	}

	// == doRender (1.8.9) split into HEAD (pre) + RETURN (post) ==
	// In 1.8.9 "isInventory" was derived from d/d1/d2/f/renderPartialTicks being zero/one. The
	// 1.20.1 PlayerRenderer.render signature no longer exposes those offsets, so the render Mixin
	// decides isInventory and passes it in. NOTE[inventory-detect]: InventoryScreen detection; verify at runtime.
	// verticalRenderOffset is the d1 nudge the original applied for crawl-sneaking non-local players;
	// the Mixin translates the PoseStack by this amount before the vanilla body runs.
	public double verticalRenderOffset;
	private SmartMoving lastMoving;
	private IModelPlayer[] lastModelPlayers;

	public void doRenderPre(AbstractClientPlayer entityplayer, float renderPartialTicks, boolean isInventory)
	{
		verticalRenderOffset = 0D;
		IModelPlayer[] modelPlayers = null;
		SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
		if(moving != null)
		{
			boolean isClimb = moving.isClimbing && !moving.isCrawling && !moving.isCrawlClimbing && !moving.isClimbJumping;
			boolean isClimbJump = moving.isClimbJumping;
			int handsClimbType = moving.actualHandsClimbType;
			int feetClimbType = moving.actualFeetClimbType;
			boolean isHandsVineClimbing = moving.isHandsVineClimbing;
			boolean isFeetVineClimbing = moving.isFeetVineClimbing;
			boolean isCeilingClimb = moving.isCeilingClimbing;
			boolean isSwim = moving.isSwimming && !moving.isDipping;
			boolean isDive = moving.isDiving;
			boolean isLevitate = moving.isLevitating;
			boolean isCrawl = moving.isCrawling && !moving.isClimbing;
			boolean isCrawlClimb = moving.isCrawlClimbing || (moving.isClimbing && moving.isCrawling);
			boolean isJump = moving.isJumping();
			boolean isHeadJump = moving.isHeadJumping;
			boolean isFlying = moving.doFlyingAnimation();
			boolean isSlide = moving.isSliding;
			boolean isFalling = moving.doFallingAnimation();
			boolean isGenericSneaking = moving.isSlow;
			boolean isAngleJumping = moving.isAngleJumping();
			int angleJumpType = moving.angleJumpType;
			boolean isRopeSliding = moving.isRopeSliding;

			SmartStatistics statistics = SmartStatisticsFactory.getInstance(entityplayer);
			float currentHorizontalSpeedFlattened = statistics != null ? statistics.getCurrentHorizontalSpeedFlattened(renderPartialTicks, -1) : Float.NaN;
			float smallOverGroundHeight = isCrawlClimb || isHeadJump ? (float)moving.getOverGroundHeight(5D) : 0F;
			BlockState overGroundBlock = isHeadJump && smallOverGroundHeight < 5F ? moving.getOverGroundBlockId(smallOverGroundHeight) : null;

			modelPlayers = irp.getMovingModels();

			for(int i = 0; i < modelPlayers.length; i++)
			{
				SmartMovingModel modelPlayer = modelPlayers[i].getMovingModel();
				modelPlayer.isClimb = isClimb;
				modelPlayer.isClimbJump = isClimbJump;
				modelPlayer.handsClimbType = handsClimbType;
				modelPlayer.feetClimbType = feetClimbType;
				modelPlayer.isHandsVineClimbing = isHandsVineClimbing;
				modelPlayer.isFeetVineClimbing = isFeetVineClimbing;
				modelPlayer.isCeilingClimb = isCeilingClimb;
				modelPlayer.isSwim = isSwim;
				modelPlayer.isDive = isDive;
				modelPlayer.isCrawl = isCrawl;
				modelPlayer.isCrawlClimb = isCrawlClimb;
				modelPlayer.isJump = isJump;
				modelPlayer.isHeadJump = isHeadJump;
				modelPlayer.isSlide = isSlide;
				modelPlayer.isFlying = isFlying;
				modelPlayer.isLevitate = isLevitate;
				modelPlayer.isFalling = isFalling;
				modelPlayer.isGenericSneaking = isGenericSneaking;
				modelPlayer.isAngleJumping = isAngleJumping;
				modelPlayer.angleJumpType = angleJumpType;
				modelPlayer.isRopeSliding = isRopeSliding;

				modelPlayer.currentHorizontalSpeedFlattened = currentHorizontalSpeedFlattened;
				modelPlayer.smallOverGroundHeight = smallOverGroundHeight;
				modelPlayer.overGroundBlock = overGroundBlock;
			}

			if (!isInventory && entityplayer.isShiftKeyDown() && !(entityplayer instanceof LocalPlayer) && isCrawl)
				verticalRenderOffset += 0.125D;
		}

		lastMoving = moving;
		lastModelPlayers = modelPlayers;

		CurrentMainModel = modelBipedMain;
	}

	public void doRenderPost()
	{
		CurrentMainModel = null;

		if (lastMoving != null && lastMoving.isLevitating && lastModelPlayers != null)
			for(int i = 0; i < lastModelPlayers.length; i++)
				lastModelPlayers[i].getMovingModel().md.currentHorizontalAngle = lastModelPlayers[i].getMovingModel().md.currentCameraAngle;
	}

	// == rotateCorpse (1.8.9) -> setupRotations pre-hook ==
	// 1.8.9 used moving.isp != null to detect the local player; that maps to LocalPlayer in 1.20.1.
	// renderYawOffset -> yBodyRot.
	public void setupRotationsPre(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2)
	{
		SmartMoving moving = SmartMovingFactory.getInstance(entityplayer);
		if(moving != null)
		{
			boolean isInventory = f2 == 1.0F && (entityplayer instanceof LocalPlayer) && Minecraft.getInstance().screen instanceof InventoryScreen;
			if(!isInventory)
			{
				float forwardRotation = entityplayer.yRotO + (entityplayer.getYRot() - entityplayer.yRotO) * f2;

				if(moving.isClimbing || moving.isClimbCrawling || moving.isCrawlClimbing || moving.isFlying || moving.isSwimming || moving.isDiving || moving.isCeilingClimbing || moving.isHeadJumping || moving.isSliding || moving.isAngleJumping())
					entityplayer.yBodyRot = forwardRotation;
			}
		}
	}

	// == renderLivingAt (1.8.9) -> PoseStack Y translation for non-local players ==
	// EntityOtherPlayerMP -> RemotePlayer, getEntityId() -> getId(). PlayerRendererMovingMixin translates
	// the PoseStack by this amount at setupRotations HEAD (the model-positioning step).
	public double renderLivingAtVerticalOffset(AbstractClientPlayer entityplayer)
	{
		if(entityplayer instanceof RemotePlayer)
		{
			SmartMoving moving = SmartMovingFactory.getOtherSmartMoving(entityplayer.getId());
			if(moving != null && moving.heightOffset != 0)
				return moving.heightOffset;
		}
		return 0D;
	}

	// == renderName (1.8.9) -> pre/post around the vanilla name render ==
	// Minecraft.isGuiEnabled() -> !options.hideGui; renderManager.livingPlayer -> camera entity;
	// isSneaking()/setSneaking() -> isShiftKeyDown()/setShiftKeyDown(). The returned value is the
	// d1 nudge the original applied to the name position; the Mixin translates the PoseStack by it.
	private boolean nameChangedIsSneaking = false;
	private boolean nameOriginalIsSneaking = false;

	public double renderNamePre(AbstractClientPlayer entityPlayer)
	{
		nameChangedIsSneaking = false;
		nameOriginalIsSneaking = false;
		double d1Delta = 0D;
		if(!Minecraft.getInstance().options.hideGui && entityPlayer != Minecraft.getInstance().getCameraEntity())
		{
			SmartMoving moving = SmartMovingFactory.getInstance(entityPlayer);
			if(moving != null)
			{
				nameOriginalIsSneaking = entityPlayer.isShiftKeyDown();
				boolean temporaryIsSneaking = nameOriginalIsSneaking;
				if(moving.isCrawling && !moving.isClimbing)
					temporaryIsSneaking = !Config._crawlNameTag.value;
				else if(nameOriginalIsSneaking)
					temporaryIsSneaking = !Config._sneakNameTag.value;

				nameChangedIsSneaking = temporaryIsSneaking != nameOriginalIsSneaking;
				if(nameChangedIsSneaking)
					entityPlayer.setShiftKeyDown(temporaryIsSneaking);

				if(moving.heightOffset == -1)
					d1Delta -= 0.2F;
				else if(nameOriginalIsSneaking && !temporaryIsSneaking)
					d1Delta -= 0.05F;
			}
		}
		return d1Delta;
	}

	public void renderNamePost(AbstractClientPlayer entityPlayer)
	{
		if(nameChangedIsSneaking)
			entityPlayer.setShiftKeyDown(nameOriginalIsSneaking);
	}

	// == renderGuiIngame (1.8.9) -> Forge in-game GUI overlay (GuiGraphics) ==
	// 1.8.9 -> 1.20.1 mapping:
	//   - takes a GuiGraphics (the 1.20.1 draw surface); registered as a Forge GUI overlay in
	//     SmartMovingClient.onRegisterGuiOverlays (RegisterGuiOverlaysEvent, registerAboveAll).
	//   - the GL11.GL_ALPHA_TEST guard and glPushAttrib/glPopAttrib/glColor4f calls protected
	//     fixed-function GL state that was removed in 1.20.1; they are dropped (blit manages its own
	//     state, the overlay event already scopes the HUD pass).
	//   - ScaledResolution -> Window.getGuiScaled{Width,Height}().
	//   - minecraft.thePlayer -> minecraft.player; playerController.shouldDrawHUD() -> !isSpectator().
	//   - isInsideOfMaterial(Material.water) -> isEyeInFluid(FluidTags.WATER); getTotalArmorValue() -> getArmorValue().
	//   - ingameGUI.drawTexturedModalRect(x,y,u,v,9,9) -> guiGraphics.blit(ICONS, x,y,u,v,9,9).
	public static void renderGuiIngame(GuiGraphics guiGraphics, Minecraft minecraft)
	{
		if (!Client.getNativeUserInterfaceDrawing())
			return;

		SmartMovingSelf moving = (SmartMovingSelf)SmartMovingFactory.getInstance(minecraft.player);
		if(moving != null && Config.enabled && (Options._displayExhaustionBar.value || Options._displayJumpChargeBar.value))
		{
			int width = minecraft.getWindow().getGuiScaledWidth();
			int height = minecraft.getWindow().getGuiScaledHeight();

			if(!minecraft.player.isSpectator())
			{
				float maxExhaustion = Client.getMaximumExhaustion();
				float exhaustion = Math.min(moving.exhaustion, maxExhaustion);
				boolean drawExhaustion = exhaustion > 0 && exhaustion <= maxExhaustion;

				float maxStillJumpCharge = Config._jumpChargeMaximum.value;
				float stillJumpCharge = Math.min(moving.jumpCharge, maxStillJumpCharge);

				float maxRunJumpCharge = Config._headJumpChargeMaximum.value;
				float runJumpCharge = Math.min(moving.headJumpCharge, maxRunJumpCharge);

				boolean drawJumpCharge = stillJumpCharge > 0 || runJumpCharge > 0;
				float maxJumpCharge = stillJumpCharge > runJumpCharge ? maxStillJumpCharge : maxRunJumpCharge;
				float jumpCharge = Math.max(stillJumpCharge, runJumpCharge);

				if(drawExhaustion || drawJumpCharge)
					_guiGraphics = guiGraphics;

				if(drawExhaustion)
				{
					float maxExhaustionForAction = Math.min(moving.maxExhaustionForAction, maxExhaustion);
					float maxExhaustionToStartAction = Math.min(moving.maxExhaustionToStartAction, maxExhaustion);

					float fitness = maxExhaustion - exhaustion;
					float minFitnessForAction = Float.isNaN(maxExhaustionForAction) ? 0 : maxExhaustion - maxExhaustionForAction;
					float minFitnessToStartAction = Float.isNaN(maxExhaustionToStartAction) ? 0 : maxExhaustion - maxExhaustionToStartAction;

					float maxFitnessDrawn = Math.max(Math.max(minFitnessToStartAction, fitness), minFitnessForAction);

					int halfs = (int)Math.floor(maxFitnessDrawn / maxExhaustion * 21F);
					int fulls = halfs / 2;
					int half = halfs % 2;

					int fitnessHalfs = (int)Math.floor(fitness / maxExhaustion * 21F);
					int fitnessFulls = fitnessHalfs / 2;
					int fitnessHalf = fitnessHalfs % 2;

					int minFitnessForActionHalfs = (int)Math.floor(minFitnessForAction / maxExhaustion * 21F);
					int minFitnessForActionFulls = minFitnessForActionHalfs / 2;
					int minFitnessForActionHalf = minFitnessForActionHalfs % 2;

					int minFitnessToStartActionHalfs = (int)Math.floor(minFitnessToStartAction / maxExhaustion * 21F);
					int minFitnessToStartActionFulls = minFitnessToStartActionHalfs / 2;

					_jOffset = height - 39 - 10 - (minecraft.player.isEyeInFluid(FluidTags.WATER) ? 10 : 0);
					for(int i = 0; i < Math.min(fulls + half, 10); i++)
					{
						_iOffset = (width / 2 + 90) - (i + 1) * 8;
						if(i < fitnessFulls)
						{
							if(i < minFitnessForActionFulls)
								drawIcon(2, 2);
							else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
								drawIcon(3, 2);
							else
								drawIcon(0, 0);
						}
						else if(i == fitnessFulls && fitnessHalf > 0)
						{
							if(i < minFitnessForActionFulls)
								drawIcon(1, 2);
							else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
								if(i < minFitnessToStartActionFulls)
									drawIcon(3, 1);
								else
									drawIcon(4, 2);
							else
								if(i < minFitnessToStartActionFulls)
									drawIcon(1, 1);
								else
									drawIcon(1, 0);
						}
						else
						{
							if(i < minFitnessForActionFulls)
								drawIcon(0, 2);
							else if(i == minFitnessForActionFulls && minFitnessForActionHalf > 0)
								if(i < minFitnessToStartActionFulls)
									drawIcon(2, 1);
								else
									drawIcon(5, 2);
							else
								if(i < minFitnessToStartActionFulls)
									drawIcon(0, 1);
								else
									drawIcon(4, 1);
						}
					}
				}

				if(drawJumpCharge)
				{
					boolean max = jumpCharge == maxJumpCharge;
					int fulls = max ? 10 : (int)Math.ceil(((jumpCharge - 2) * 10D) / maxJumpCharge);
					int half = max ? 0 : (int)Math.ceil((jumpCharge * 10D) / maxJumpCharge) - fulls;

					_jOffset = height - 39 - 10 - (minecraft.player.getArmorValue() > 0 ? 10 : 0);
					for(int i = 0; i < fulls + half; i++)
					{
						_iOffset = (width / 2 - 91) + i * 8;
						drawIcon(i < fulls ? 2 : 3, 0);
					}
				}
			}
		}
	}

	private static void drawIcon(int x, int y)
	{
		_guiGraphics.blit(ICONS, _iOffset, _jOffset, x * 9, y * 9, 9, 9);
	}

	public final SmartMovingModel modelBipedMain;

	private static final ResourceLocation ICONS = new ResourceLocation("smartmoving", "gui/icons.png");
	private static int _iOffset, _jOffset;
	private static GuiGraphics _guiGraphics;
}
