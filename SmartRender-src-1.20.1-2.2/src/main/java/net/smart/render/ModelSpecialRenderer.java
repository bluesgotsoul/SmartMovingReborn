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

// 1.8.9 -> 1.20.1 PORT NOTE
// The (modelBase, i, j) constructor arguments existed only to build boxes through the
// vanilla ModelRenderer texture-offset machinery (display lists). In 1.20.1 the geometry
// is captured from a vanilla ModelPart via ModelRotationRenderer.setGeometry, so they are
// kept on the signature for caller compatibility (ModelCapeRenderer / ModelEarsRenderer)
// but are no longer used here. glPopMatrix/glPushMatrix -> the shared CurrentPoseStack.
public class ModelSpecialRenderer extends ModelRotationRenderer
{
	public boolean doPopPush;

	public ModelSpecialRenderer(Model modelBase, int i, int j, ModelRotationRenderer baseRenderer)
	{
		super(modelBase, i, j, baseRenderer);
		ignoreRender = true;
	}

	public void beforeRender(boolean popPush)
	{
		doPopPush = popPush;
		ignoreRender = false;
	}

	@Override
	public void doRender(float f, boolean useParentTransformations)
	{
		if(doPopPush)
		{
			CurrentPoseStack.popPose();
			CurrentPoseStack.pushPose();
		}
		super.doRender(f, true);
	}

	public void afterRender()
	{
		ignoreRender = true;
		doPopPush = false;
	}
}
