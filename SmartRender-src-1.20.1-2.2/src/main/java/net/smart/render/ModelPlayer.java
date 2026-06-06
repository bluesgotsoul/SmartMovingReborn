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
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// ------------------------------------------------------------------
// 1.8.9 -> 1.20.1 PORT NOTE
//
// 1.8.9: extends net.minecraft.client.model.ModelPlayer and implemented IModelPlayer. The
// cape and deadmau5 (ear) renderers were injected into the vanilla model through reflection
// (Reflect.SetField(_bipedCape / _bipedDeadmau5Head, ...)) and renderCape() was overridden
// to route the vanilla cape pass through Smart Render.
//
// 1.20.1:
//   * the model is baked from a PlayerModel mesh (createMesh(deformation, slim) -> bakeRoot),
//   * PlayerModel exposes the cape/ear parts as public final fields (cloak / ear), so the
//     reflection in SmartRenderInstall is no longer needed; their cube geometry is captured
//     into Smart Render's ModelCapeRenderer / ModelEarsRenderer,
//   * the vanilla cape is drawn by net.minecraft.client.renderer.entity.layers.CapeLayer,
//     not by the model, so renderCape()/superRenderCloak() are routed from a CapeLayer mixin
//     NOTE[cape-layer]: CapeLayerMixin wraps CapeLayer.render with bipedCloak.before/afterRender.
// ------------------------------------------------------------------
public class ModelPlayer<T extends LivingEntity> extends PlayerModel<T> implements IModelPlayer
{
	private final SmartRenderModel model;

	public ModelPlayer(HumanoidModel<?> existing, float f, boolean b)
	{
		super(bakeRoot(f, b), b);

		// 1.8.9 read these via reflection; PlayerModel exposes them directly in 1.20.1.
		ModelPart bipedCape = ((net.smart.render.mixin.PlayerModelAccessor)this).smartrender$getCloak();
		ModelPart bipedDeadmau5Head = ((net.smart.render.mixin.PlayerModelAccessor)this).smartrender$getEar();

		model = new SmartRenderModel(b, this, this, existing.body, this.jacket, existing.head, existing.hat, existing.rightArm, this.rightSleeve, existing.leftArm, this.leftSleeve, existing.rightLeg, this.rightPants, existing.leftLeg, this.leftPants, bipedCape, bipedDeadmau5Head);
	}

	// 1.8.9 super(f, b): f = box expand, b = small (slim) arms. CubeDeformation carries the
	// expand and PlayerModel.createMesh carries the slim flag in 1.20.1.
	private static ModelPart bakeRoot(float f, boolean b)
	{
		return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(f), b), 64, 64).bakeRoot();
	}

	// ------------------------------------------------------------------
	// 1.8.9 -> 1.20.1 vanilla render-pipeline bridge
	//
	// 1.8.9: net.smart.render.ModelPlayer.setRotationAngles(6 floats, Entity) and
	// render(Entity, 6 floats) shared the SAME signatures as the vanilla model methods, so they
	// overrode the vanilla calls directly. In 1.20.1 the vanilla entry points are
	// setupAnim(entity, 5 floats) and renderToBuffer(PoseStack, VertexConsumer, ...), so the
	// hooks below forward into the Smart Render methods. The trailing "factor" (model scale) was
	// 0.0625F in vanilla and is reused. The entity and limb parameters are stashed in setupAnim
	// because renderToBuffer no longer receives them.
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
		// See ModelBiped.superRender: faithful no-op (vanilla skeleton suppressed, Smart Render
		// draws the captured geometry through bipedOuter).
	}

	@Override
	public SmartRenderModel getRenderModel()
	{
		return model;
	}

	@Override
	public void initialize(ModelRotationRenderer bipedBody, ModelRotationRenderer bipedBodywear, ModelRotationRenderer bipedHead, ModelRotationRenderer bipedHeadwear, ModelRotationRenderer bipedRightArm, ModelRotationRenderer bipedRightArmwear, ModelRotationRenderer bipedLeftArm, ModelRotationRenderer bipedLeftArmwear, ModelRotationRenderer bipedRightLeg, ModelRotationRenderer bipedRightLegwear, ModelRotationRenderer bipedLeftLeg, ModelRotationRenderer bipedLeftLegwear, ModelCapeRenderer bipedCloak, ModelEarsRenderer bipedEars)
	{
		// 1.8.9 reassigned the vanilla wear fields and reflected the cape/ear renderers into the
		// vanilla model. In 1.20.1 the parts are final and SmartRenderModel already owns the
		// bones + cape/ear renderers, so no field swap is required.
	}

	public void setRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
	{
		model.setRotationAngles(totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset, factor, entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void superSetRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity)
	{
		super.setupAnim((T) entity, totalHorizontalDistance, currentHorizontalSpeed, totalTime, viewHorizontalAngelOffset, viewVerticalAngelOffset);
		ModelBiped.copyPart(head, model.bipedHead);
		ModelBiped.copyPart(body, model.bipedBody);
		ModelBiped.copyPart(rightArm, model.bipedRightArm);
		ModelBiped.copyPart(leftArm, model.bipedLeftArm);
		ModelBiped.copyPart(rightLeg, model.bipedRightLeg);
		ModelBiped.copyPart(leftLeg, model.bipedLeftLeg);
	}

	// 1.8.9 overrode net.minecraft.client.model.ModelPlayer.renderCape(f). In 1.20.1 the cape
	// is drawn by CapeLayer; this hook is invoked from the CapeLayer mixin instead of being a
	// model override. NOTE[cape-layer]: CapeLayerMixin wraps CapeLayer.render with bipedCloak.before/afterRender -- faithful 1.20.1 equivalent.
	public void renderCape(float f)
	{
		model.renderCloak(f);
	}

	@Override
	public void superRenderCloak(float f)
	{
		// 1.8.9 called super.renderCape(f) (the vanilla cape pass). In 1.20.1 the vanilla cape is
		// a separate CapeLayer; the actual draw is performed by the CapeLayer mixin in the
		// NOTE[cape-layer]: CapeLayerMixin handles the vanilla cape draw via before/afterRender.
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
}
