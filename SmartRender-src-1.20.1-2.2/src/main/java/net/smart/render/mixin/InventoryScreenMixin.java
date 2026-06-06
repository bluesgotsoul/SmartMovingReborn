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
// along with Smart Render. If not, see.
// ==================================================================

package net.smart.render.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.smart.render.SmartRenderRender;

// 1.8.9 -> 1.20.1 PORT NOTE:
// Vanilla draws the inventory/horse entity preview via the STATIC method
// InventoryScreen.renderEntityInInventoryFollowsMouse(...): it temporarily overrides the entity's
// yBodyRot/yRot/xRot/yHeadRot to face the viewer, calls the entity render dispatcher, then restores
// them. We raise SmartRenderRender.renderingInventory for exactly that window so SmartRenderRender
// (doRenderPre / setupRotationsPre) can reliably tell a GUI preview from a world render. This replaces
// the old partialTicks==1.0F heuristic, which never holds in 1.20.1's GUI render path and made the
// inventory take the world-rotation branch (180° flip when facing north, jitter, and a leak of the
// rotation into the world via the shared RendererData).
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("HEAD"))
    private static void smartrender$guiPreviewStart(CallbackInfo ci) {
        SmartRenderRender.renderingInventory = true;
    }

    @Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("RETURN"))
    private static void smartrender$guiPreviewEnd(CallbackInfo ci) {
        SmartRenderRender.renderingInventory = false;
    }
}