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

import com.mojang.math.Axis;

import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

// 1.8.9 -> 1.20.1 PORT NOTE
// Field mapping (matches vanilla CapeLayer in 1.20.1):
//   prevChasingPos{X,Y,Z} -> {x,y,z}CloakO   chasingPos{X,Y,Z} -> {x,y,z}Cloak
//   prevPos{X,Y,Z} -> {x,y,z}o               pos{X,Y,Z} -> get{X,Y,Z}()
//   prevRenderYawOffset -> yBodyRotO          renderYawOffset -> yBodyRot
//   prevCameraYaw -> oBob                     cameraYaw -> bob
//   prevDistanceWalkedModified -> walkDistO   distanceWalkedModified -> walkDist
//   MathHelper -> Mth, isSneaking() -> isCrouching()
//   GL11.glTranslatef / GlStateManager.translate -> CurrentPoseStack.translate
//   GL11.glRotatef(deg, axis) -> CurrentPoseStack.mulPose(Axis.*P.rotationDegrees(deg))
// The lerp/clamp arithmetic is kept exactly as the original SmartMoving code wrote it.
public class ModelCapeRenderer extends ModelSpecialRenderer {
	public ModelCapeRenderer(Model modelBase, int i, int j, ModelRotationRenderer baseRenderer,
			ModelRotationRenderer outerRenderer) {
		super(modelBase, i, j, baseRenderer);
		outer = outerRenderer;
	}

	public void beforeRender(AbstractClientPlayer entityplayer, float factor) {
		this.entityplayer = entityplayer;
		this.setFactor = factor;

		super.beforeRender(true);
	}

	@Override
	public void preTransform(float factor, boolean push) {
		if (entityplayer.isCrouching())
			CurrentPoseStack.translate(0.0F, 0.2F, 0.0F);

		super.preTransform(factor, push);
		boolean teleported = Mth.degreesDifferenceAbs(entityplayer.yBodyRotO, entityplayer.yBodyRot) > 90.0F
				|| Math.abs(entityplayer.getX() - entityplayer.xo) > 4.0D
				|| Math.abs(entityplayer.getY() - entityplayer.yo) > 4.0D
				|| Math.abs(entityplayer.getZ() - entityplayer.zo) > 4.0D;
		float f = teleported ? 1.0F : setFactor;

		double d = (entityplayer.xCloakO + (entityplayer.xCloak - entityplayer.xCloakO) * f)
				- (entityplayer.xo + (entityplayer.getX() - entityplayer.xo) * f);
		double d1 = (entityplayer.yCloakO + (entityplayer.yCloak - entityplayer.yCloakO) * f)
				- (entityplayer.yo + (entityplayer.getY() - entityplayer.yo) * f);
		double d2 = (entityplayer.zCloakO + (entityplayer.zCloak - entityplayer.zCloakO) * f)
				- (entityplayer.zo + (entityplayer.getZ() - entityplayer.zo) * f);
		float f1 = teleported ? entityplayer.yBodyRot
				: (entityplayer.yBodyRotO + (entityplayer.yBodyRot - entityplayer.yBodyRotO) * setFactor);
		double d3 = Mth.sin((f1 * 3.141593F) / 180F);
		double d4 = -Mth.cos((f1 * 3.141593F) / 180F);
		float f2 = (float) d1 * 10F;
		if (f2 < -6F)
			f2 = -6F;
		if (f2 > 32F)
			f2 = 32F;
		float f3 = (float) (d * d3 + d2 * d4) * 100F;
		float f4 = (float) (d * d4 - d2 * d3) * 100F;
		if (f3 < 0.0F)
			f3 = 0.0F;
		float f5 = entityplayer.oBob + (entityplayer.bob - entityplayer.oBob) * setFactor;
		f2 += Mth.sin((entityplayer.walkDistO + (entityplayer.walkDist - entityplayer.walkDistO) * setFactor) * 6F)
				* 32F * f5;

		if (entityplayer.isCrouching()) {
			CurrentPoseStack.translate(0.0F, 0.009F, 0.044F);
			f2 -= 9.24F;
		} else
			f2 -= 5.62F;

		float localAngle = 6F + f3 / 2.0F + f2;
		float localAngleMax = Math.max(70.523F - outer.rotateAngleX * RadiantToAngle, 6F);
		float realLocalAngle = Math.min(localAngle, localAngleMax);

		CurrentPoseStack.mulPose(Axis.XP.rotationDegrees(realLocalAngle));
		CurrentPoseStack.mulPose(Axis.ZP.rotationDegrees(f4 / 2.0F));
		CurrentPoseStack.mulPose(Axis.YP.rotationDegrees(-f4 / 2.0F));
		CurrentPoseStack.mulPose(Axis.YP.rotationDegrees(180F));
	}

	@Override
	public boolean canBeRandomBoxSource() {
		return false;
	}

	private final ModelRotationRenderer outer;
	private AbstractClientPlayer entityplayer;
	private float setFactor;
}
