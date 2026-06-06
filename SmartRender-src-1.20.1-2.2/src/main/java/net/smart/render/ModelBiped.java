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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// ------------------------------------------------------------------
// 1.8.9 -> 1.20.1 PORT NOTE
//
// 1.8.9: extends net.minecraft.client.model.ModelBiped (the armor / generic biped model)
// and implemented IModelPlayer. initialize() reassigned the vanilla ModelRenderer fields
// (this.bipedBody = ourBone, ...) so that the vanilla render path drew Smart Render's bones.
//
// 1.20.1: HumanoidModel's part fields (head/body/rightArm/...) are FINAL ModelPart and
// cannot be reassigned, and the vanilla model is built from a baked ModelPart root instead
// of a float-expand constructor. So:
//   * the constructor bakes a humanoid root (createMesh -> LayerDefinition -> bakeRoot) and
//     hands SmartRenderModel the vanilla parts of "existing"; SmartRenderModel captures
//     their cube geometry into its own bone hierarchy (setGeometry),
//   * initialize() can no longer swap fields, so it is a no-op (SmartRenderModel already
//     owns the bones); the vanilla skeleton is suppressed and Smart Render draws everything
//     through bipedOuter using the captured geometry,
//   * superSetRotationAngles mirrors the vanilla flat pose back onto the bones (the legal
//     equivalent of the old field swap) for the first-person / inventory path.
// ------------------------------------------------------------------
public class ModelBiped<T extends LivingEntity> extends HumanoidModel<T> implements IModelPlayer
{
	private final SmartRenderModel model;

	public ModelBiped(HumanoidModel<?> existing, float f)
	{
		super(bakeRoot(f));

		model = new SmartRenderModel(false, this, this, existing.body, null, existing.head, existing.hat, existing.rightArm, null, existing.leftArm, null, existing.rightLeg, null, existing.leftLeg, null, null, null);
	}

	// 1.8.9 super(f): f was the vanilla "expand" used to grow boxes (armor layers). The same
	// growth is expressed through CubeDeformation when baking the model in 1.20.1.
	private static ModelPart bakeRoot(float f)
	{
		return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(f), 0.0F), 64, 32).bakeRoot();
	}

	// ------------------------------------------------------------------
	// 1.8.9 -> 1.20.1 vanilla render-pipeline bridge (armor / generic biped model)
	//
	// Same rationale as net.smart.render.ModelPlayer: setRotationAngles/render shared the
	// vanilla signatures in 1.8.9 and overrode them; in 1.20.1 we forward setupAnim ->
	// setRotationAngles and renderToBuffer -> render, stashing the entity + limb parameters
	// because renderToBuffer no longer receives them. factor = vanilla model scale 0.0625F.
	// ------------------------------------------------------------------
	private Entity smartEntity;
	private float smartHorizontalDistance;
	private float smartHorizontalSpeed;
	private float smartTotalTime;
	private float smartViewHorizontalAngle;
	private float smartViewVerticalAngle;

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
	{
		smartEntity = entity;
		smartHorizontalDistance = limbSwing;
		smartHorizontalSpeed = limbSwingAmount;
		smartTotalTime = ageInTicks;
		smartViewHorizontalAngle = netHeadYaw;
		smartViewVerticalAngle = headPitch;
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, entity);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
	{
		if (smartEntity == null)
		{
			super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			return;
		}

		PoseStack previousPoseStack = ModelRotationRenderer.CurrentPoseStack;
		VertexConsumer previousBuffer = ModelRotationRenderer.CurrentBuffer;
		int previousLight = ModelRotationRenderer.CurrentLight;
		int previousOverlay = ModelRotationRenderer.CurrentOverlay;

		ModelRotationRenderer.CurrentPoseStack = poseStack;
		ModelRotationRenderer.CurrentBuffer = buffer;
		ModelRotationRenderer.CurrentLight = packedLight;
		ModelRotationRenderer.CurrentOverlay = packedOverlay;

		render(smartEntity, smartHorizontalDistance, smartHorizontalSpeed, smartTotalTime, smartViewHorizontalAngle, smartViewVerticalAngle, 0.0625F);

		ModelRotationRenderer.CurrentPoseStack = previousPoseStack;
		ModelRotationRenderer.CurrentBuffer = previousBuffer;
		ModelRotationRenderer.CurrentLight = previousLight;
		ModelRotationRenderer.CurrentOverlay = previousOverlay;
	}

	public void render(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.render(entity, totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor);
	}

	@Override
	public void superRender(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		// 1.8.9 called super.render(...) while every Smart Render bone was flagged ignoreRender,
		// so the vanilla pass drew nothing visible (the real skeleton is drawn from captured
		// geometry in SmartRenderModel.render via bipedOuter). The vanilla 1.20.1 skeleton is
		// likewise suppressed, so this is a faithful no-op.
	}

	@Override
	public SmartRenderModel getRenderModel()
	{
		return model;
	}

	@Override
	public void initialize(ModelRotationRenderer bipedBody, ModelRotationRenderer bipedBodywear, ModelRotationRenderer bipedHead, ModelRotationRenderer bipedHeadwear, ModelRotationRenderer bipedRightArm, ModelRotationRenderer bipedRightArmwear, ModelRotationRenderer bipedLeftArm, ModelRotationRenderer bipedLeftArmwear, ModelRotationRenderer bipedRightLeg, ModelRotationRenderer bipedRightLegwear, ModelRotationRenderer bipedLeftLeg, ModelRotationRenderer bipedLeftLegwear, ModelCapeRenderer bipedCloak, ModelEarsRenderer bipedEars)
	{
		// 1.8.9 reassigned the vanilla ModelRenderer fields here so the vanilla render path used
		// Smart Render's bones. In 1.20.1 those fields are final ModelPart; SmartRenderModel
		// already owns the bones and draws them directly, so no field swap is needed.
	}

	public void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
	{
		model.setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void superSetRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
	{
		// 1.8.9 super.setRotationAngles(...) posed the (swapped-in) bones directly. In 1.20.1
		// vanilla setupAnim poses the vanilla parts, so the resulting flat pose is mirrored back
		// onto Smart Render's bones (used by the first-person / inventory path).
		super.setupAnim((T) entity, totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset);
		copyVanillaPose(model);
	}

	@Override
	public void superRenderCloak(float f)
	{
	}

	public ModelPart getRandomModelPart(RandomSource random)
	{
		return model.getRandomBox(random);
	}

	@Override public ModelRotationRenderer getOuter() { return model.bipedOuter; }
	@Override public ModelRotationRenderer getTorso() { return model.bipedTorso; }
	@Override public ModelRotationRenderer getBody() { return model.bipedBody; }
	@Override public ModelRotationRenderer getBreast() { return model.bipedBreast; }
	@Override public ModelRotationRenderer getNeck() { return model.bipedNeck; }
	@Override public ModelRotationRenderer getRenderHead() { return model.bipedHead; }
	@Override public ModelRotationRenderer getHeadwear() { return model.bipedHeadwear; }
	@Override public ModelRotationRenderer getRightShoulder() { return model.bipedRightShoulder; }
	@Override public ModelRotationRenderer getRightArm() { return model.bipedRightArm; }
	@Override public ModelRotationRenderer getLeftShoulder() { return model.bipedLeftShoulder; }
	@Override public ModelRotationRenderer getLeftArm() { return model.bipedLeftArm; }
	@Override public ModelRotationRenderer getPelvic() { return model.bipedPelvic; }
	@Override public ModelRotationRenderer getRightLeg() { return model.bipedRightLeg; }
	@Override public ModelRotationRenderer getLeftLeg() { return model.bipedLeftLeg; }
	@Override public ModelRotationRenderer getEars() { return model.bipedEars; }
	@Override public ModelRotationRenderer getCloak() { return model.bipedCloak; }

	@Override
	public void animateHeadRotation(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateHeadRotation(viewHorizontalAngelOffset, viewVerticalAngelOffset);
	}

	@Override
	public void animateSleeping(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateSleeping();
	}

	@Override
	public void animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateArmSwinging(totalHorizontalDistance, currentHorizontalSpeed);
	}

	@Override
	public void animateRiding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateRiding();
	}

	@Override
	public void animateLeftArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateLeftArmItemHolding();
	}

	@Override
	public void animateRightArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateRightArmItemHolding();
	}

	@Override
	public void animateWorkingBody(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateWorkingBody();
	}

	@Override
	public void animateWorkingArms(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateWorkingArms();
	}

	@Override
	public void animateSneaking(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateSneaking();
	}

	@Override
	public void animateArms(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateArms(totalTime);
	}

	@Override
	public void animateBowAiming(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor)
	{
		model.animateBowAiming(totalTime);
	}

	// Mirror the vanilla flat pose (head/body/arms/legs) onto Smart Render's bones. This is the
	// legal 1.20.1 equivalent of the 1.8.9 field swap, used by the first-person / inventory
	// branch where Smart Render delegates posing to vanilla.
	void copyVanillaPose(SmartRenderModel m)
	{
		copyPart(head, m.bipedHead);
		copyPart(body, m.bipedBody);
		copyPart(rightArm, m.bipedRightArm);
		copyPart(leftArm, m.bipedLeftArm);
		copyPart(rightLeg, m.bipedRightLeg);
		copyPart(leftLeg, m.bipedLeftLeg);
	}

	static void copyPart(ModelPart from, ModelRotationRenderer to)
	{
		if(from == null || to == null)
			return;
		to.rotateAngleX = from.xRot;
		to.rotateAngleY = from.yRot;
		to.rotateAngleZ = from.zRot;
		to.rotationPointX = from.x;
		to.rotationPointY = from.y;
		to.rotationPointZ = from.z;
	}
}
