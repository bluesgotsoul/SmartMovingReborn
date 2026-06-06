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

import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

// 1.8.9 -> 1.20.1 PORT NOTE
//   isSneaking() -> isCrouching(), EntityPlayerSP -> LocalPlayer,
//   rotationPitch -> getXRot(), GL11.glTranslate*/glScalef -> CurrentPoseStack.translate/scale.
public class ModelEarsRenderer extends ModelSpecialRenderer
{
	private int _i = 0;
	private AbstractClientPlayer entityplayer;

	public ModelEarsRenderer(Model modelBase, int i, int j, ModelRotationRenderer baseRenderer)
	{
		super(modelBase, i, j, baseRenderer);
	}

	public void beforeRender(AbstractClientPlayer entityplayer)
	{
		super.beforeRender(true);
		this.entityplayer = entityplayer;
	}

	@Override
	public void doRender(float f, boolean useParentTransformations)
	{
		reset();
		super.doRender(f, useParentTransformations);
	}

	@Override
	public void preTransform(float factor, boolean push)
	{
		if(entityplayer.isCrouching())
			CurrentPoseStack.translate(0.0F, 0.2F * (entityplayer instanceof LocalPlayer ? Math.cos(entityplayer.getXRot() / RadiantToAngle) : 1), 0.0F);

		super.preTransform(factor, push);

		int i = _i++ % 2;
		CurrentPoseStack.translate(0.375F * (i * 2 - 1), 0.0F, 0.0F);
		CurrentPoseStack.translate(0.0F, -0.375F, 0.0F);
		CurrentPoseStack.scale(1.333333F, 1.333333F, 1.333333F);
	}

	@Override
	public boolean canBeRandomBoxSource()
	{
		return false;
	}
}
