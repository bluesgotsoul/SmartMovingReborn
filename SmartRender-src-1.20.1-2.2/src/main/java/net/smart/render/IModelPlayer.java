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

import net.minecraft.world.entity.Entity;

// 1.8.9 -> 1.20.1 PORT NOTE
// The getter return type ModelRenderer becomes ModelRotationRenderer: in 1.20.1 the
// vanilla ModelPart is final and cannot be subclassed, so Smart Render's bones are
// standalone ModelRotationRenderer instances. net.minecraft.entity.Entity -> net.minecraft.world.entity.Entity.
public interface IModelPlayer
{
	SmartRenderModel getRenderModel();

	void initialize(ModelRotationRenderer bipedBody, ModelRotationRenderer bipedBodywear, ModelRotationRenderer bipedHead, ModelRotationRenderer bipedHeadwear, ModelRotationRenderer bipedRightArm, ModelRotationRenderer bipedRightArmwear, ModelRotationRenderer bipedLeftArm, ModelRotationRenderer bipedLeftArmwear, ModelRotationRenderer bipedRightLeg, ModelRotationRenderer bipedRightLegwear, ModelRotationRenderer bipedLeftLeg, ModelRotationRenderer bipedLeftLegwear, ModelCapeRenderer bipedCloak, ModelEarsRenderer bipedEars);

	void superRender(Entity entity, float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);

	void superSetRotationAngles(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor, Entity entity);

	void superRenderCloak(float f);

	ModelRotationRenderer getOuter();
	ModelRotationRenderer getTorso();
	ModelRotationRenderer getBody();
	ModelRotationRenderer getBreast();
	ModelRotationRenderer getNeck();
	ModelRotationRenderer getRenderHead();
	ModelRotationRenderer getHeadwear();
	ModelRotationRenderer getRightShoulder();
	ModelRotationRenderer getRightArm();
	ModelRotationRenderer getLeftShoulder();
	ModelRotationRenderer getLeftArm();
	ModelRotationRenderer getPelvic();
	ModelRotationRenderer getRightLeg();
	ModelRotationRenderer getLeftLeg();
	ModelRotationRenderer getEars();
	ModelRotationRenderer getCloak();

	void animateHeadRotation(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateSleeping(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateArmSwinging(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateRiding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateLeftArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateRightArmItemHolding(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateWorkingBody(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateWorkingArms(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateSneaking(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateArms(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
	void animateBowAiming(float totalHorizontalDistance, float currentHorizontalSpeed, float totalTime, float viewHorizontalAngelOffset, float viewVerticalAngelOffset, float factor);
}
