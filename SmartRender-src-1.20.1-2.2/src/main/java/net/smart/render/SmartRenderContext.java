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

// ------------------------------------------------------------------
// 1.8.9 -> 1.20.1 PORT NOTE
//
// In 1.8.9 this class exposed:
//
//   public static <T extends Render<AbstractClientPlayer>> void registerRenderers(Class<T> type)
//   { SmartRenderFactory.registerRenderers(type); }
//
// which swapped the vanilla player renderer at runtime (via reflection on RenderManager)
// for Smart Render's own Render<AbstractClientPlayer> subclass.
//
// 1.20.1 has no Render<AbstractClientPlayer> and renderers cannot be swapped by reflection:
// player rendering is registered through EntityRenderersEvent.AddLayers (and a PlayerModel
// mixin for the model itself). That registration therefore lives in the render-registration
// layer (SmartRenderFactory / SmartRenderInstall port), not here.
//
// This class is kept (SmartRenderModel/SmartRenderRender extend it) so the SmartRenderUtilities
// math constants stay reachable through the original inheritance chain.
// NOTE[render-registration]: EntityRenderersEvent.AddLayers is NOT needed.
// PlayerRendererMixin is applied at class-load time to all PlayerRenderer instances;
// no renderer swap or AddLayers handler is required in the Mixin architecture.
// ------------------------------------------------------------------
public abstract class SmartRenderContext extends SmartRenderUtilities
{
}
