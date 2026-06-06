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
import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

// ------------------------------------------------------------------
// 1.8.9 -> 1.20.1 PORT NOTE
//
// In 1.8.9 this class "extends ModelRenderer" and rendered through GL11 display
// lists, reaching into ModelRenderer's private compiled/compileDisplayList/displayList
// via reflection, and using the raw GL matrix stack (glPushMatrix/glTranslatef/
// glRotatef/glScalef/glCallList). It implemented its OWN base/child transform
// hierarchy with a configurable rotation order and fade interpolation.
//
// In 1.20.1 net.minecraft.client.model.geom.ModelPart is FINAL and rendering is
// PoseStack + VertexConsumer based, so this can no longer extend the vanilla part.
// Instead it is a standalone "bone" that:
//   * keeps the exact same transform state + math as the original, and
//   * drives a shared PoseStack (the moral equivalent of the old global GL matrix
//     stack) directly, replicating preTransform/rotate/postTransform 1:1, then
//   * delegates cube drawing to a captured, transform-less ModelPart (geometry only).
//
// The shared PoseStack/VertexConsumer/light/overlay live in static "Current*" fields
// because the 1.8.9 code relied on the single global GL state; client rendering is
// single threaded, so this mirrors the original behaviour faithfully.
// ------------------------------------------------------------------
public class ModelRotationRenderer
{
	protected final static float RadiantToAngle = SmartRenderUtilities.RadiantToAngle;
	protected final static float Whole = SmartRenderUtilities.Whole;
	protected final static float Half = SmartRenderUtilities.Half;

	public static int XYZ = 0;
	public static int XZY = 1;
	public static int YXZ = 2;
	public static int YZX = 3;
	public static int ZXY = 4;
	public static int ZYX = 5;

	// Shared render context = the old global GL state.
	public static PoseStack CurrentPoseStack;
	public static VertexConsumer CurrentBuffer;
	public static int CurrentLight;
	public static int CurrentOverlay;

	// Captured cube geometry (no children, identity transform). null = pure group bone.
	public ModelPart part;

	protected ModelRotationRenderer base;
	public final List<ModelRotationRenderer> childModels = new ArrayList<ModelRotationRenderer>();

	// 1.8.9 ModelRenderer fields that this class relied on (pixel units).
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	public float offsetX;
	public float offsetY;
	public float offsetZ;
	public boolean mirror;
	public boolean isHidden;
	public boolean showModel = true;

	public boolean ignoreRender;
	public boolean forceRender;

	public int rotationOrder;

	public float scaleX;
	public float scaleY;
	public float scaleZ;

	public boolean ignoreBase;
	public boolean ignoreSuperRotation;

	public boolean fadeEnabled;

	public boolean fadeOffsetX;
	public boolean fadeOffsetY;
	public boolean fadeOffsetZ;
	public boolean fadeRotateAngleX;
	public boolean fadeRotateAngleY;
	public boolean fadeRotateAngleZ;
	public boolean fadeRotationPointX;
	public boolean fadeRotationPointY;
	public boolean fadeRotationPointZ;

	public RendererData previous;

	public ModelRotationRenderer(ModelRotationRenderer baseRenderer)
	{
		rotationOrder = XYZ;

		base = baseRenderer;
		if(base != null)
			base.addChild(this);

		scaleX = 1.0F;
		scaleY = 1.0F;
		scaleZ = 1.0F;

		fadeEnabled = false;
	}

	// 1.8.9 callers used (ModelBase, int textureOffsetX, int textureOffsetY, base). The
	// texture offsets only drove vanilla box generation (display lists), which is gone now
	// (geometry is captured via setGeometry), so they are accepted for caller compatibility
	// and ignored.
	public ModelRotationRenderer(Model modelBase, int i, int j, ModelRotationRenderer baseRenderer)
	{
		this(baseRenderer);
	}

	public void addChild(ModelRotationRenderer child)
	{
		childModels.add(child);
	}

	public void setRotationPoint(float x, float y, float z)
	{
		rotationPointX = x;
		rotationPointY = y;
		rotationPointZ = z;
	}

	// Capture the cubes of a vanilla ModelPart as transform-less geometry. SmartRender
	// rebuilds the skeleton hierarchy itself, so only the cubes are kept (no children),
	// and this bone drives every transform via the PoseStack.
	// Requires the access transformer that exposes ModelPart.cubes.
	public void setGeometry(ModelPart source)
	{
		if(source == null)
			return;
		part = new ModelPart(new ArrayList<>(((net.smart.render.mixin.ModelPartAccessor)(Object)source).smartrender$getCubes()), new HashMap<String, ModelPart>());
	}

	public void render(float f)
	{
		if((!ignoreRender && !ignoreBase) || forceRender)
			doRender(f, ignoreBase);
	}

	public void renderIgnoreBase(float f)
	{
		if(ignoreBase)
			doRender(f, false);
	}

	public void doRender(float f, boolean useParentTransformations)
	{
		if(!preRender(f))
			return;
		preTransforms(f, true, useParentTransformations);
		drawGeometry();
		for(int i = 0; i < childModels.size(); i++)
			childModels.get(i).render(f);
		postTransforms(f, true, useParentTransformations);
	}

	// Replaces GL11.glCallList(displayList): the captured geometry part is at identity,
	// so it only emits its cubes into the current PoseStack/buffer.
	private void drawGeometry()
	{
		if(part == null || CurrentPoseStack == null || CurrentBuffer == null)
			return;
		part.render(CurrentPoseStack, CurrentBuffer, CurrentLight, CurrentOverlay);
	}

	public boolean preRender(float f)
	{
		if(isHidden)
			return false;

		if(!showModel)
			return false;

		return true;
	}

	public void preTransforms(float f, boolean push, boolean useParentTransformations)
	{
		if(base != null && !ignoreBase && useParentTransformations)
			base.preTransforms(f, push, true);
		preTransform(f, push);
	}

	public void preTransform(float f, boolean push)
	{
		if(rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F || ignoreSuperRotation)
		{
			if(push)
				CurrentPoseStack.pushPose();

			CurrentPoseStack.translate(rotationPointX * f, rotationPointY * f, rotationPointZ * f);

			if(ignoreSuperRotation)
			{
				// 1.8.9 read the model-view matrix, dropped its rotation and kept only the
				// translation ("glLoadIdentity" + "glTranslatef"). Reproduce on the PoseStack.
				Matrix4f matrix = CurrentPoseStack.last().pose();
				Vector3f translation = matrix.getTranslation(new Vector3f());
				matrix.identity();
				matrix.setTranslation(translation);
				CurrentPoseStack.last().normal().identity();
			}

			rotate(rotationOrder, rotateAngleX, rotateAngleY, rotateAngleZ);

			CurrentPoseStack.scale(scaleX, scaleY, scaleZ);
			CurrentPoseStack.translate(offsetX, offsetY, offsetZ);
		}
		else if(rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F || scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F || offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F)
		{
			CurrentPoseStack.translate(rotationPointX * f, rotationPointY * f, rotationPointZ * f);
			CurrentPoseStack.scale(scaleX, scaleY, scaleZ);
			CurrentPoseStack.translate(offsetX, offsetY, offsetZ);
		}
	}

	private static void rotate(int rotationOrder, float rotateAngleX, float rotateAngleY, float rotateAngleZ)
	{
		// glRotatef took degrees (angle * RadiantToAngle); Axis.*.rotation() takes radians,
		// so the raw radian angles are passed through. The call sequence is preserved 1:1.
		if((rotationOrder == ZXY) && rotateAngleY != 0.0F)
			CurrentPoseStack.mulPose(Axis.YP.rotation(rotateAngleY));

		if((rotationOrder == YXZ) && rotateAngleZ != 0.0F)
			CurrentPoseStack.mulPose(Axis.ZP.rotation(rotateAngleZ));

		if((rotationOrder == YZX || rotationOrder == YXZ || rotationOrder == ZXY || rotationOrder == ZYX) && rotateAngleX != 0.0F)
			CurrentPoseStack.mulPose(Axis.XP.rotation(rotateAngleX));

		if((rotationOrder == XZY || rotationOrder == ZYX) && rotateAngleY != 0.0F)
			CurrentPoseStack.mulPose(Axis.YP.rotation(rotateAngleY));

		if((rotationOrder == XYZ || rotationOrder == XZY || rotationOrder == YZX || rotationOrder == ZXY || rotationOrder == ZYX) && rotateAngleZ != 0.0F)
			CurrentPoseStack.mulPose(Axis.ZP.rotation(rotateAngleZ));

		if((rotationOrder == XYZ || rotationOrder == YXZ || rotationOrder == YZX) && rotateAngleY != 0.0F)
			CurrentPoseStack.mulPose(Axis.YP.rotation(rotateAngleY));

		if((rotationOrder == XYZ || rotationOrder == XZY) && rotateAngleX != 0.0F)
			CurrentPoseStack.mulPose(Axis.XP.rotation(rotateAngleX));
	}

	public void postTransform(float f, boolean pop)
	{
		if(rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F || ignoreSuperRotation)
		{
			if(pop)
				CurrentPoseStack.popPose();
		}
		else if(rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F || scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F || offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F)
		{
			CurrentPoseStack.translate(-offsetX, -offsetY, -offsetZ);
			CurrentPoseStack.scale(1F / scaleX, 1F / scaleY, 1F / scaleZ);
			CurrentPoseStack.translate(-rotationPointX * f, -rotationPointY * f, -rotationPointZ * f);
		}
	}

	public void postTransforms(float f, boolean pop, boolean useParentTransformations)
	{
		postTransform(f, pop);
		if(base != null && !ignoreBase && useParentTransformations)
			base.postTransforms(f, pop, true);
	}

	public void reset()
	{
		rotationOrder = XYZ;

		scaleX = 1.0F;
		scaleY = 1.0F;
		scaleZ = 1.0F;

		rotationPointX = 0F;
		rotationPointY = 0F;
		rotationPointZ = 0F;

		rotateAngleX = 0F;
		rotateAngleY = 0F;
		rotateAngleZ = 0F;

		ignoreBase = false;
		ignoreSuperRotation = false;
		forceRender = false;

		offsetX = 0;
		offsetY = 0;
		offsetZ = 0;

		fadeOffsetX = false;
		fadeOffsetY = false;
		fadeOffsetZ = false;
		fadeRotateAngleX = false;
		fadeRotateAngleY = false;
		fadeRotateAngleZ = false;
		fadeRotationPointX = false;
		fadeRotationPointY = false;
		fadeRotationPointZ = false;

		previous = null;
	}

	// 1.8.9 overrode ModelRenderer.renderWithRotation/postRender (vanilla hooks used to
	// attach held items / extra layers to a bone). Without the vanilla base class these
	// are now plain transform helpers driving the shared PoseStack.
	public void renderWithRotation(float f)
	{
		if(!preRender(f))
			return;
		CurrentPoseStack.pushPose();
		CurrentPoseStack.translate(rotationPointX * f, rotationPointY * f, rotationPointZ * f);
		rotate(rotationOrder, rotateAngleX, rotateAngleY, rotateAngleZ);
		drawGeometry();
		CurrentPoseStack.popPose();
	}

	public void postRender(float f)
	{
		if(!preRender(f))
			return;
		preTransforms(f, false, true);
	}

	public RendererData previous()
	{
		return previous;
	}

	public void fadeStore(float totalTime)
	{
		if(previous != null)
		{
			previous.offsetX = offsetX;
			previous.offsetY = offsetY;
			previous.offsetZ = offsetZ;
			previous.rotateAngleX = rotateAngleX;
			previous.rotateAngleY = rotateAngleY;
			previous.rotateAngleZ = rotateAngleZ;
			previous.rotationPointX = rotationPointX;
			previous.rotationPointY = rotationPointY;
			previous.rotationPointZ = rotationPointZ;
			previous.totalTime = totalTime;
		}
	}

	public void fadeIntermediate(float totalTime)
	{
		if(previous != null && totalTime - previous.totalTime <= 2F)
		{
			offsetX = GetIntermediatePosition(previous.offsetX, offsetX, fadeOffsetX, previous.totalTime, totalTime);
			offsetY = GetIntermediatePosition(previous.offsetY, offsetY, fadeOffsetY, previous.totalTime, totalTime);
			offsetZ = GetIntermediatePosition(previous.offsetZ, offsetZ, fadeOffsetZ, previous.totalTime, totalTime);

			rotateAngleX = GetIntermediateAngle(previous.rotateAngleX, rotateAngleX, fadeRotateAngleX, previous.totalTime, totalTime);
			rotateAngleY = GetIntermediateAngle(previous.rotateAngleY, rotateAngleY, fadeRotateAngleY, previous.totalTime, totalTime);
			rotateAngleZ = GetIntermediateAngle(previous.rotateAngleZ, rotateAngleZ, fadeRotateAngleZ, previous.totalTime, totalTime);

			rotationPointX = GetIntermediatePosition(previous.rotationPointX, rotationPointX, fadeRotationPointX, previous.totalTime, totalTime);
			rotationPointY = GetIntermediatePosition(previous.rotationPointY, rotationPointY, fadeRotationPointY, previous.totalTime, totalTime);
			rotationPointZ = GetIntermediatePosition(previous.rotationPointZ, rotationPointZ, fadeRotationPointZ, previous.totalTime, totalTime);
		}
	}

	@SuppressWarnings("static-method")
	public boolean canBeRandomBoxSource()
	{
		return true;
	}

	private static float GetIntermediatePosition(float prevPosition, float shouldPosition, boolean fade, float lastTotalTime, float totalTime)
	{
		if(!fade || shouldPosition == prevPosition)
			return shouldPosition;

		return prevPosition + (shouldPosition - prevPosition) * (totalTime - lastTotalTime) * 0.2F;
	}

	private static float GetIntermediateAngle(float prevAngle, float shouldAngle, boolean fade, float lastTotalTime, float totalTime)
	{
		if(!fade || shouldAngle == prevAngle)
			return shouldAngle;

		while(prevAngle >= Whole) prevAngle -= Whole;
		while(prevAngle < 0F) prevAngle += Whole;

		while(shouldAngle >= Whole) shouldAngle -= Whole;
		while(shouldAngle < 0F) shouldAngle += Whole;

		if(shouldAngle > prevAngle && (shouldAngle - prevAngle) > Half)
			prevAngle += Whole;

		if(shouldAngle < prevAngle && (prevAngle - shouldAngle) > Half)
			shouldAngle += Whole;

		return prevAngle + (shouldAngle - prevAngle) * (totalTime - lastTotalTime) * 0.2F;
	}
}
