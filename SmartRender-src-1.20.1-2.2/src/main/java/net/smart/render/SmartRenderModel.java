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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

// Smart Render threads the active PoseStack through this static (the old GL11 global state);
// it is owned by ModelRotationRenderer and set by the model render-pipeline bridge.
import static net.smart.render.ModelRotationRenderer.CurrentPoseStack;

// ------------------------------------------------------------------
// 1.8.9 -> 1.20.1 PORT NOTE
//
// net.minecraft.client.model.ModelBiped     -> HumanoidModel<?>
// net.minecraft.client.model.ModelPlayer    -> PlayerModel<?>
// net.minecraft.client.model.ModelRenderer  -> ModelRotationRenderer (Smart Render bone) /
//                                              net.minecraft.client.model.geom.ModelPart (vanilla originals)
// net.minecraft.util.MathHelper             -> net.minecraft.util.Mth (sqrt_float -> sqrt)
// net.minecraft.entity.Entity               -> net.minecraft.world.entity.Entity
// raw GL11 matrix stack                     -> ModelRotationRenderer.CurrentPoseStack
// mp.isRiding/isSneak/swingProgress/aimedBow-> mp.riding/crouching/attackTime/(leftArmPose|rightArmPose)
// mp.heldItemLeft/heldItemRight (int)       -> heldItemPose(mp.leftArmPose|rightArmPose) (see note)
// entity.ridingEntity                       -> entity.getVehicle()
//
// The geometry of each vanilla part is captured into the matching bone via setGeometry
// (replaces the 1.8.9 ModelRenderer cubeList copy + display-list compilation). The
// transform skeleton, render order and fade math are kept exactly as the original.
// ------------------------------------------------------------------
public class SmartRenderModel extends SmartRenderContext
{
	public IModelPlayer imp;
	public HumanoidModel<?> mp;
	public boolean isModelPlayer;
	public boolean smallArms;

	public SmartRenderModel(boolean b, HumanoidModel<?> mb, IModelPlayer imp, ModelPart originalBipedBody, ModelPart originalBipedBodywear, ModelPart originalBipedHead, ModelPart originalBipedHeadwear, ModelPart originalBipedRightArm, ModelPart originalBipedRightArmwear, ModelPart originalBipedLeftArm, ModelPart originalBipedLeftArmwear, ModelPart originalBipedRightLeg, ModelPart originalBipedRightLegwear, ModelPart originalBipedLeftLeg, ModelPart originalBipedLeftLegwear, ModelPart originalBipedCape, ModelPart originalBipedDeadmau5Head)
	{
		this.imp = imp;
		this.mp = mb;

		isModelPlayer = mp instanceof PlayerModel;
		smallArms = b;

		// 1.8.9 did mb.boxList.clear() to stop the vanilla model from auto-rendering its own
		// parts (Smart Render takes over rendering through its own bone hierarchy). HumanoidModel
		// has no boxList in 1.20.1; vanilla part auto-render is suppressed at the PlayerModel
		// renderToBuffer mixin in the render-registration layer instead.

		bipedOuter = create(null);
		bipedOuter.fadeEnabled = true;

		bipedTorso = create(bipedOuter);
		bipedBody = create(bipedTorso, originalBipedBody);
		bipedBreast = create(bipedTorso);
		bipedNeck = create(bipedBreast);
		bipedHead = create(bipedNeck, originalBipedHead);
		bipedRightShoulder = create(bipedBreast);
		bipedRightArm = create(bipedRightShoulder, originalBipedRightArm);
		bipedLeftShoulder = create(bipedBreast);
		bipedLeftShoulder.mirror = true;
		bipedLeftArm = create(bipedLeftShoulder, originalBipedLeftArm);
		bipedPelvic = create(bipedTorso);
		bipedRightLeg = create(bipedPelvic, originalBipedRightLeg);
		bipedLeftLeg = create(bipedPelvic, originalBipedLeftLeg);

		bipedBodywear = create(bipedBody, originalBipedBodywear);
		bipedHeadwear = create(bipedHead, originalBipedHeadwear);
		bipedRightArmwear = create(bipedRightArm, originalBipedRightArmwear);
		bipedLeftArmwear = create(bipedLeftArm, originalBipedLeftArmwear);
		bipedRightLegwear = create(bipedRightLeg, originalBipedRightLegwear);
		bipedLeftLegwear = create(bipedLeftLeg, originalBipedLeftLegwear);

		if(originalBipedCape != null)
		{
			bipedCloak = new ModelCapeRenderer(mb, 0, 0, bipedBreast, bipedOuter);
			copy(bipedCloak, originalBipedCape);
		}

		if(originalBipedDeadmau5Head != null)
		{
			bipedEars = new ModelEarsRenderer(mb, 24, 0, bipedHead);
			copy(bipedEars, originalBipedDeadmau5Head);
		}

		reset(); // set default rotation points

		imp.initialize(bipedBody, bipedBodywear, bipedHead, bipedHeadwear, bipedRightArm, bipedRightArmwear, bipedLeftArm, bipedLeftArmwear, bipedRightLeg, bipedRightLegwear, bipedLeftLeg, bipedLeftLegwear, bipedCloak, bipedEars);

		if(SmartRenderRender.CurrentMainModel != null)
		{
			isInventory = SmartRenderRender.CurrentMainModel.isInventory;

			totalVerticalDistance = SmartRenderRender.CurrentMainModel.totalVerticalDistance;
			currentVerticalSpeed = SmartRenderRender.CurrentMainModel.currentVerticalSpeed;
			totalDistance = SmartRenderRender.CurrentMainModel.totalDistance;
			currentSpeed = SmartRenderRender.CurrentMainModel.currentSpeed;

			distance = SmartRenderRender.CurrentMainModel.distance;
			verticalDistance = SmartRenderRender.CurrentMainModel.verticalDistance;
			horizontalDistance = SmartRenderRender.CurrentMainModel.horizontalDistance;
			currentCameraAngle = SmartRenderRender.CurrentMainModel.currentCameraAngle;
			currentVerticalAngle = SmartRenderRender.CurrentMainModel.currentVerticalAngle;
			currentHorizontalAngle = SmartRenderRender.CurrentMainModel.currentHorizontalAngle;
			prevOuterRenderData = SmartRenderRender.CurrentMainModel.prevOuterRenderData;
			isSleeping = SmartRenderRender.CurrentMainModel.isSleeping;

			actualRotation = SmartRenderRender.CurrentMainModel.actualRotation;
			forwardRotation = SmartRenderRender.CurrentMainModel.forwardRotation;
			workingAngle = SmartRenderRender.CurrentMainModel.workingAngle;
		}
	}

	private ModelRotationRenderer create(ModelRotationRenderer base)
	{
		return new ModelRotationRenderer(mp, -1, -1, base);
	}

	private ModelRotationRenderer create(ModelRotationRenderer base, ModelPart original)
	{
		if(original == null)
			return null;

		// 1.8.9 read textureOffsetX/Y off the vanilla ModelRenderer and forwarded them to the
		// constructor (used to regenerate boxes). In 1.20.1 the geometry is captured directly
		// from the part's already-built cubes, so the texture offsets are no longer needed.
		ModelRotationRenderer local = new ModelRotationRenderer(mp, -1, -1, base);
		copy(local, original);
		return local;
	}

	private static void copy(ModelRotationRenderer local, ModelPart original)
	{
		// 1.8.9 copied childModels + cubeList individually and the mirror/isHidden/showModel
		// flags. In 1.20.1 the vanilla player parts are flat cube containers, so the part's
		// cubes are captured directly and only the visibility flag has an equivalent
		// (ModelPart.visible). Per-cube mirror is already baked into the captured geometry.
		local.setGeometry(original);
		local.showModel = original.visible;
	}

	public void render(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		CurrentPoseStack.pushPose();
		if (entity.isCrouching())
			CurrentPoseStack.translate(0.0F, 0.2F, 0.0F);

		bipedBody.ignoreRender = bipedHead.ignoreRender = bipedRightArm.ignoreRender = bipedLeftArm.ignoreRender = bipedRightLeg.ignoreRender = bipedLeftLeg.ignoreRender = true;
		if (isModelPlayer)
			bipedBodywear.ignoreRender = bipedHeadwear.ignoreRender = bipedRightArmwear.ignoreRender = bipedLeftArmwear.ignoreRender = bipedRightLegwear.ignoreRender = bipedLeftLegwear.ignoreRender = true;
		imp.superRender(entity, totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
		if (isModelPlayer)
			bipedBodywear.ignoreRender = bipedHeadwear.ignoreRender = bipedRightArmwear.ignoreRender = bipedLeftArmwear.ignoreRender = bipedRightLegwear.ignoreRender = bipedLeftLegwear.ignoreRender = false;
		bipedBody.ignoreRender = bipedHead.ignoreRender = bipedRightArm.ignoreRender = bipedLeftArm.ignoreRender = bipedRightLeg.ignoreRender = bipedLeftLeg.ignoreRender = false;

		bipedOuter.render(factor);

		bipedOuter.renderIgnoreBase(factor);
		bipedTorso.renderIgnoreBase(factor);
		bipedBody.renderIgnoreBase(factor);
		bipedBreast.renderIgnoreBase(factor);
		bipedNeck.renderIgnoreBase(factor);
		bipedHead.renderIgnoreBase(factor);
		bipedRightShoulder.renderIgnoreBase(factor);
		bipedRightArm.renderIgnoreBase(factor);
		bipedLeftShoulder.renderIgnoreBase(factor);
		bipedLeftArm.renderIgnoreBase(factor);
		bipedPelvic.renderIgnoreBase(factor);
		bipedRightLeg.renderIgnoreBase(factor);
		bipedLeftLeg.renderIgnoreBase(factor);

		if (isModelPlayer)
		{
			bipedBodywear.renderIgnoreBase(factor);
			bipedHeadwear.renderIgnoreBase(factor);
			bipedRightArmwear.renderIgnoreBase(factor);
			bipedLeftArmwear.renderIgnoreBase(factor);
			bipedRightLegwear.renderIgnoreBase(factor);
			bipedLeftLegwear.renderIgnoreBase(factor);
		}

		CurrentPoseStack.popPose();
	}

	public void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
	{
		reset();

		if(firstPerson || isInventory)
		{
			bipedBody.ignoreBase = true;
			bipedHead.ignoreBase = true;
			bipedRightArm.ignoreBase = true;
			bipedLeftArm.ignoreBase = true;
			bipedRightLeg.ignoreBase = true;
			bipedLeftLeg.ignoreBase = true;

			if (isModelPlayer)
			{
				bipedBodywear.ignoreBase = true;
				bipedHeadwear.ignoreBase = true;
				bipedRightArmwear.ignoreBase = true;
				bipedLeftArmwear.ignoreBase = true;
				bipedRightLegwear.ignoreBase = true;
				bipedLeftLegwear.ignoreBase = true;

				bipedEars.ignoreBase = true;
				bipedCloak.ignoreBase = true;
			}

			bipedBody.forceRender = firstPerson;
			bipedHead.forceRender = firstPerson;
			bipedRightArm.forceRender = firstPerson;
			bipedLeftArm.forceRender = firstPerson;
			bipedRightLeg.forceRender = firstPerson;
			bipedLeftLeg.forceRender = firstPerson;

			if (isModelPlayer)
			{
				bipedBodywear.forceRender = firstPerson;
				bipedHeadwear.forceRender = firstPerson;
				bipedRightArmwear.forceRender = firstPerson;
				bipedLeftArmwear.forceRender = firstPerson;
				bipedRightLegwear.forceRender = firstPerson;
				bipedLeftLegwear.forceRender = firstPerson;

				bipedEars.forceRender = firstPerson;
				bipedCloak.forceRender = firstPerson;
			}

			bipedRightArm.setRotationPoint(-5F, 2.0F, 0.0F);
			bipedLeftArm.setRotationPoint(5F, 2.0F, 0.0F);
			bipedRightLeg.setRotationPoint(-2F, 12F, 0.0F);
			bipedLeftLeg.setRotationPoint(2.0F, 12F, 0.0F);

			imp.superSetRotationAngles(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
			return;
		}

		if(isSleeping)
		{
			prevOuterRenderData.rotateAngleX = 0;
			prevOuterRenderData.rotateAngleY = 0;
			prevOuterRenderData.rotateAngleZ = 0;
		}

		bipedOuter.previous = prevOuterRenderData;

		bipedOuter.rotateAngleY = actualRotation / RadiantToAngle;
		bipedOuter.fadeRotateAngleY = entity.getVehicle() == null;

		imp.animateHeadRotation(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(isSleeping)
			imp.animateSleeping(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		imp.animateArmSwinging(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(mp.riding)
			imp.animateRiding(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(heldItemPose(mp.leftArmPose) != 0)
			imp.animateLeftArmItemHolding(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(heldItemPose(mp.rightArmPose) != 0)
			imp.animateRightArmItemHolding(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(mp.attackTime > -9990F)
		{
			imp.animateWorkingBody(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
			imp.animateWorkingArms(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
		}

		if(mp.crouching)
			imp.animateSneaking(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		imp.animateArms(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(isAimedBow(mp))
			imp.animateBowAiming(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);

		if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleX)
			bipedOuter.previous.rotateAngleX = bipedOuter.rotateAngleX;

		if(bipedOuter.previous != null && !bipedOuter.fadeRotateAngleY)
			bipedOuter.previous.rotateAngleY = bipedOuter.rotateAngleY;

		bipedOuter.fadeIntermediate(totalTime);
		bipedOuter.fadeStore(totalTime);

		if (isModelPlayer)
		{
			bipedCloak.ignoreBase = false;
			bipedCloak.rotateAngleX = Sixtyfourth;
		}
	}

	public void animateHeadRotation(float viewHorizontalAngelOffset, float viewVerticalAngelOffset)
	{
		bipedNeck.ignoreBase = true;
		bipedHead.rotateAngleY = (actualRotation + viewHorizontalAngelOffset) / RadiantToAngle;
		bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;
	}

	public void animateSleeping()
	{
		bipedNeck.ignoreBase = false;
		bipedHead.rotateAngleY = 0F;
		bipedHead.rotateAngleX = Eighth;
		bipedTorso.rotationPointZ = -17F;
	}

	public void animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed)
	{
		bipedRightArm.rotateAngleX = Mth.cos(totalHorizontalDistance * 0.6662F + Half) * 2.0F * currentHorizontalSpeed * 0.5F;
		bipedLeftArm.rotateAngleX = Mth.cos(totalHorizontalDistance * 0.6662F) * 2.0F * currentHorizontalSpeed * 0.5F;

		bipedRightLeg.rotateAngleX = Mth.cos(totalHorizontalDistance * 0.6662F) * 1.4F * currentHorizontalSpeed;
		bipedLeftLeg.rotateAngleX = Mth.cos(totalHorizontalDistance * 0.6662F + Half) * 1.4F * currentHorizontalSpeed;
	}

	public void animateRiding()
	{
		bipedRightArm.rotateAngleX += -0.6283185F;
		bipedLeftArm.rotateAngleX += -0.6283185F;
		bipedRightLeg.rotateAngleX = -1.256637F;
		bipedLeftLeg.rotateAngleX = -1.256637F;
		bipedRightLeg.rotateAngleY = 0.3141593F;
		bipedLeftLeg.rotateAngleY = -0.3141593F;
	}

	public void animateLeftArmItemHolding()
	{
		bipedLeftArm.rotateAngleX = bipedLeftArm.rotateAngleX * 0.5F - 0.3141593F * heldItemPose(mp.leftArmPose);
	}

	public void animateRightArmItemHolding()
	{
		int heldItemRight = heldItemPose(mp.rightArmPose);
		bipedRightArm.rotateAngleX = bipedRightArm.rotateAngleX * 0.5F - 0.3141593F * heldItemRight;
		if(heldItemRight == 3)
			bipedRightArm.rotateAngleY = -0.5235988F;
	}

	public void animateWorkingBody()
	{
		float angle = Mth.sin(Mth.sqrt(mp.attackTime) * Whole) * 0.2F;
		bipedBreast.rotateAngleY = bipedBody.rotateAngleY += angle;
		bipedBreast.rotationOrder = bipedBody.rotationOrder = ModelRotationRenderer.YXZ;
		bipedLeftArm.rotateAngleX += angle;
	}

	public void animateWorkingArms()
	{
		float f6 = 1.0F - mp.attackTime;
		f6 = 1.0F - f6 * f6 * f6;
		float f7 = Mth.sin(f6 * Half);
		float f8 = Mth.sin(mp.attackTime * Half) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
		bipedRightArm.rotateAngleX -= f7 * 1.2D + f8;
		bipedRightArm.rotateAngleY += Mth.sin(Mth.sqrt(mp.attackTime) * Whole) * 0.4F;
		bipedRightArm.rotateAngleZ -= Mth.sin(mp.attackTime * Half) * 0.4F;
	}

	public void animateSneaking()
	{
		bipedTorso.rotateAngleX += 0.5F;
		bipedRightLeg.rotateAngleX += -0.5F;
		bipedLeftLeg.rotateAngleX += -0.5F;
		bipedRightArm.rotateAngleX += -0.1F;
		bipedLeftArm.rotateAngleX += -0.1F;

		bipedPelvic.offsetY = -0.13652F;
		bipedPelvic.offsetZ = -0.05652F;

		bipedBreast.offsetY = -0.01872F;
		bipedBreast.offsetZ = -0.07502F;

		bipedNeck.offsetY = 0.0621F;
	}

	public void animateArms(float totalTime)
	{
		bipedRightArm.rotateAngleZ += Mth.cos(totalTime * 0.09F) * 0.05F + 0.05F;
		bipedLeftArm.rotateAngleZ -= Mth.cos(totalTime * 0.09F) * 0.05F + 0.05F;
		bipedRightArm.rotateAngleX += Mth.sin(totalTime * 0.067F) * 0.05F;
		bipedLeftArm.rotateAngleX -= Mth.sin(totalTime * 0.067F) * 0.05F;
	}

	public void animateBowAiming(float totalTime)
	{
		bipedRightArm.rotateAngleZ = 0.0F;
		bipedLeftArm.rotateAngleZ = 0.0F;
		bipedRightArm.rotateAngleY = -0.1F + bipedHead.rotateAngleY - bipedOuter.rotateAngleY;
		bipedLeftArm.rotateAngleY = 0.1F + bipedHead.rotateAngleY + 0.4F - bipedOuter.rotateAngleY;
		bipedRightArm.rotateAngleX = -1.570796F + bipedHead.rotateAngleX;
		bipedLeftArm.rotateAngleX = -1.570796F + bipedHead.rotateAngleX;
		bipedRightArm.rotateAngleZ += Mth.cos(totalTime * 0.09F) * 0.05F + 0.05F;
		bipedLeftArm.rotateAngleZ -= Mth.cos(totalTime * 0.09F) * 0.05F + 0.05F;
		bipedRightArm.rotateAngleX += Mth.sin(totalTime * 0.067F) * 0.05F;
		bipedLeftArm.rotateAngleX -= Mth.sin(totalTime * 0.067F) * 0.05F;
	}

	public void reset()
	{
		bipedOuter.reset();
		bipedTorso.reset();
		bipedBody.reset();
		bipedBreast.reset();
		bipedNeck.reset();
		bipedHead.reset();
		bipedRightShoulder.reset();
		bipedRightArm.reset();
		bipedLeftShoulder.reset();
		bipedLeftArm.reset();
		bipedPelvic.reset();
		bipedRightLeg.reset();
		bipedLeftLeg.reset();

		if (isModelPlayer)
		{
			bipedBodywear.reset();
			bipedHeadwear.reset();
			bipedRightArmwear.reset();
			bipedLeftArmwear.reset();
			bipedRightLegwear.reset();
			bipedLeftLegwear.reset();

			bipedEars.reset();
			bipedCloak.reset();
		}

		bipedRightShoulder.setRotationPoint(-5F, isModelPlayer && smallArms ? 2.5F : 2.0F, 0.0F);
		bipedLeftShoulder.setRotationPoint(5F, isModelPlayer && smallArms ? 2.5F : 2.0F, 0.0F);
		bipedPelvic.setRotationPoint(0.0F, 12.0F, 0.1F);
		bipedRightLeg.setRotationPoint(-1.9F, 0.0F, 0.0F);
		bipedLeftLeg.setRotationPoint(1.9F, 0.0F, 0.0F);

		if (isModelPlayer)
			bipedCloak.setRotationPoint(0.0F, 0.0F, 2.0F);
	}

	public void renderCloak(float f)
	{
		attemptToCallRenderCape = true;
		if(!disabled)
			imp.superRenderCloak(f);
	}

	// 1.8.9 iterated the vanilla model's boxList (which, after boxList.clear(), held the
	// Smart Render bones because each ModelRenderer auto-registered itself). Bones no longer
	// auto-register to a list in 1.20.1, so the equivalent is to iterate our explicit bones.
	// Returns a vanilla ModelPart (matches Model.getRandomModelPart(RandomSource) in 1.20.1).
	public ModelPart getRandomBox(RandomSource par1Random)
	{
		List<ModelRotationRenderer> boxList = boneList();
		int size = boxList.size();
		int renderersWithBoxes = 0;

		for(int i=0; i<size; i++)
		{
			ModelRotationRenderer renderer = boxList.get(i);
			if(canBeRandomBoxSource(renderer))
				renderersWithBoxes++;
		}

		if(renderersWithBoxes != 0)
		{
			int random = par1Random.nextInt(renderersWithBoxes);
			renderersWithBoxes = -1;

			for(int i=0; i<size; i++)
			{
				ModelRotationRenderer renderer = boxList.get(i);
				if(canBeRandomBoxSource(renderer))
					renderersWithBoxes++;
				if(renderersWithBoxes == random)
					return renderer.part;
			}
		}

		return null;
	}

	private List<ModelRotationRenderer> boneList()
	{
		List<ModelRotationRenderer> list = new ArrayList<ModelRotationRenderer>();
		list.add(bipedOuter);
		list.add(bipedTorso);
		list.add(bipedBody);
		list.add(bipedBreast);
		list.add(bipedNeck);
		list.add(bipedHead);
		list.add(bipedRightShoulder);
		list.add(bipedRightArm);
		list.add(bipedLeftShoulder);
		list.add(bipedLeftArm);
		list.add(bipedPelvic);
		list.add(bipedRightLeg);
		list.add(bipedLeftLeg);
		list.add(bipedBodywear);
		list.add(bipedHeadwear);
		list.add(bipedRightArmwear);
		list.add(bipedLeftArmwear);
		list.add(bipedRightLegwear);
		list.add(bipedLeftLegwear);
		list.add(bipedEars);
		list.add(bipedCloak);
		return list;
	}

	private static boolean canBeRandomBoxSource(ModelRotationRenderer renderer)
	{
		return renderer != null && renderer.part != null && !((net.smart.render.mixin.ModelPartAccessor)(Object)renderer.part).smartrender$getCubes().isEmpty() && renderer.canBeRandomBoxSource();
	}

	// 1.8.9 -> 1.20.1: ModelBiped.heldItemLeft/heldItemRight were ints (0 = empty hand,
	// 1 = item, 3 = block-like). 1.20.1 replaced them with the HumanoidModel.ArmPose enum, so
	// map back to the legacy ints the animate*ItemHolding hooks were written against.
	// NOTE[arm-pose-int]: only EMPTY/BLOCK/other are distinguished; faithful 1.20.1 mapping -- revisit if finer pose needed.
	private static int heldItemPose(HumanoidModel.ArmPose pose)
	{
		if(pose == null || pose == HumanoidModel.ArmPose.EMPTY)
			return 0;
		if(pose == HumanoidModel.ArmPose.BLOCK)
			return 3;
		return 1;
	}

	private static boolean isAimedBow(HumanoidModel<?> mp)
	{
		return mp.rightArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW || mp.leftArmPose == HumanoidModel.ArmPose.BOW_AND_ARROW;
	}

	public boolean isInventory;

	public int scaleArmType;
	public int scaleLegType;

	public float totalVerticalDistance;
	public float currentVerticalSpeed;
	public float totalDistance;
	public float currentSpeed;

	public double distance;
	public double verticalDistance;
	public double horizontalDistance;
	public float currentCameraAngle;
	public float currentVerticalAngle;
	public float currentHorizontalAngle;

	public float actualRotation;
	public float forwardRotation;
	public float workingAngle;

	public ModelRotationRenderer bipedOuter;
	public ModelRotationRenderer bipedTorso;
	public ModelRotationRenderer bipedBody;
	public ModelRotationRenderer bipedBreast;
	public ModelRotationRenderer bipedNeck;
	public ModelRotationRenderer bipedHead;
	public ModelRotationRenderer bipedRightShoulder;
	public ModelRotationRenderer bipedRightArm;
	public ModelRotationRenderer bipedLeftShoulder;
	public ModelRotationRenderer bipedLeftArm;
	public ModelRotationRenderer bipedPelvic;
	public ModelRotationRenderer bipedRightLeg;
	public ModelRotationRenderer bipedLeftLeg;

	public ModelRotationRenderer bipedBodywear;
	public ModelRotationRenderer bipedHeadwear;
	public ModelRotationRenderer bipedRightArmwear;
	public ModelRotationRenderer bipedLeftArmwear;
	public ModelRotationRenderer bipedRightLegwear;
	public ModelRotationRenderer bipedLeftLegwear;

	public ModelEarsRenderer bipedEars;
	public ModelCapeRenderer bipedCloak;


	public boolean disabled;
	public boolean attemptToCallRenderCape;
	public RendererData prevOuterRenderData;
	public boolean isSleeping;
	public boolean firstPerson;
}
