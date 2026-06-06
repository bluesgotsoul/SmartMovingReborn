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

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// 1.8.9 -> 1.20.1 PORT NOTE:
//   The old RenderPlayer used Reflect.SetField(LayerArmorBase._modelArmorChestplate / _modelArmor, ..)
//   to swap the armor layer's two models for Smart Render's bone-driven models. In 1.20.1 the two
//   models are HumanoidArmorLayer.innerModel (worn close, the old _modelArmor, expand 0.5) and
//   outerModel (the old _modelArmorChestplate, expand 1.0). Both are private final, so they are
//   exposed here with @Mutable setters (plus getters for the IRenderPlayer accessors).
@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor
{
	@Accessor("innerModel")
	HumanoidModel<?> smartrender$getInnerModel();

	@Accessor("outerModel")
	HumanoidModel<?> smartrender$getOuterModel();

	@Mutable
	@Accessor("innerModel")
	void smartrender$setInnerModel(HumanoidModel<?> model);

	@Mutable
	@Accessor("outerModel")
	void smartrender$setOuterModel(HumanoidModel<?> model);
}
