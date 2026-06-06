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

package net.smart.moving;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class Button extends SmartMovingContext
{
	public boolean Pressed;
	public boolean WasPressed;

	public boolean StartPressed;
	public boolean StopPressed;

	public void update(KeyMapping binding)
	{
		update(Minecraft.getInstance().isWindowActive() && isKeyDown(binding));
	}

	public void update(int keyCode)
	{
		update(Minecraft.getInstance().isWindowActive() && isKeyDown(keyCode));
	}

	public void update(boolean pressed)
	{
		WasPressed = Pressed;
		Pressed = pressed;

		StartPressed = !WasPressed && Pressed;
		StopPressed = WasPressed && !Pressed;
	}

	private static boolean isKeyDown(KeyMapping keyBinding)
	{
		return isKeyDown(keyBinding, keyBinding.consumeClick());
	}

	private static boolean isKeyDown(KeyMapping keyBinding, boolean wasDown)
	{
		Screen currentScreen = Minecraft.getInstance().screen;
		// 1.8.9 also let input through for screens with GuiScreen.allowUserInput; that field has no 1.20.1 equivalent.
		if(currentScreen == null)
			return isKeyDown(keyBinding.getKey());
		return wasDown;
	}

	private static boolean isKeyDown(InputConstants.Key key)
	{
		long window = Minecraft.getInstance().getWindow().getWindow();
		if(key.getType() == InputConstants.Type.MOUSE)
			return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
		return InputConstants.isKeyDown(window, key.getValue());
	}

	private static boolean isKeyDown(int keyCode)
	{
		long window = Minecraft.getInstance().getWindow().getWindow();
		if(keyCode >= 0)
			return InputConstants.isKeyDown(window, keyCode);
		return GLFW.glfwGetMouseButton(window, keyCode + 100) == GLFW.GLFW_PRESS;
	}
}
