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

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   In 1.8.9 this interface was implemented by a RenderPlayer subclass, so it also exposed
//   superDoRender / superRotateCorpse / superRenderSpecials -> direct super.* calls.
//   In 1.20.1 the implementation is a Mixin on PlayerRenderer (see PlayerRendererMixin), and a
//   Mixin cannot expose a callable "super.*" of the target's parent. The "super" behaviour is
//   therefore reproduced by letting the original (vanilla) method body run between the
//   SmartRenderRender pre/post hooks, so those three methods are intentionally dropped here.
//   Everything else is a faithful 1:1 mapping:
//     ModelBiped  -> HumanoidModel   ModelPlayer -> PlayerModel   RenderManager -> EntityRenderDispatcher
public interface IRenderPlayer
{
	IModelPlayer createModel(HumanoidModel<?> existing, float f, boolean b);

	void initialize(PlayerModel<?> modelBipedMain, HumanoidModel<?> mb, HumanoidModel<?> mb2);

	EntityRenderDispatcher getRenderRenderManager();

	PlayerModel<?> getModelBipedMain();

	HumanoidModel<?> getModelArmorChestplate();

	HumanoidModel<?> getModelArmor();

	boolean getSmallArms();

	IModelPlayer[] getRenderModels();
}
