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

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   In 1.8.9 this interface was implemented by a RenderPlayer subclass, so it also exposed
//   superRenderDoRender / superRenderRotateCorpse / superRenderRenderLivingAt / superRenderRenderName
//   -> direct super.* calls. In 1.20.1 the implementation is a Mixin on PlayerRenderer (see the
//   render mixin), and a Mixin cannot expose a callable "super.*" of the target's parent. That
//   "super" behaviour is reproduced by letting the vanilla method body run between the
//   SmartMovingRender pre/post hooks, so those four super* methods are intentionally dropped here
//   (mirrors net.smart.render.IRenderPlayer).
//   Faithful 1:1 mapping otherwise: RenderManager -> EntityRenderDispatcher.
public interface IRenderPlayer
{
	EntityRenderDispatcher getMovingRenderManager();

	IModelPlayer getMovingModelBipedMain();

	IModelPlayer getMovingModelArmorChestplate();

	IModelPlayer getMovingModelArmor();

	IModelPlayer[] getMovingModels();

	// 1.8.9 -> 1.20.1 PORT NOTE:
	//   1.8.9 kept the SmartMovingRender as a `private final` field on
	//   net.smart.moving.render.RenderPlayer and read it straight from the render hooks. In 1.20.1 the
	//   render hooks live in a separate PlayerRenderer.render mixin, so the renderer exposes its
	//   SmartMovingRender through this interface instead of a field. SmartRenderRenderMixin sets it once,
	//   right after it builds the SmartMovingRender.
	SmartMovingRender getMovingRender();

	void setMovingRender(SmartMovingRender render);
}
