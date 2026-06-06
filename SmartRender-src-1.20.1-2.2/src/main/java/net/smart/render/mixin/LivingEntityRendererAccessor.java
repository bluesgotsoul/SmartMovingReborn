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

package net.smart.render.mixin;

import java.util.List;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   The old RenderPlayer set its main model (mainModel = modelBipedMain) and iterated
//   this.layerRenderers to re-target the armor / head layer models. In 1.20.1 those members
//   live on LivingEntityRenderer as the protected fields "model" and "layers". This accessor
//   exposes them so PlayerRendererMixin.initialize(..) can perform the same wiring without
//   reflection.
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor
{
	@Accessor("model")
	void smartrender$setModel(EntityModel<?> model);

	@Accessor("layers")
	List<RenderLayer<?, ?>> smartrender$getLayers();
}
