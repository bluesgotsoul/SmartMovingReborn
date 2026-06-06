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

package net.smart.utilities;

import com.mojang.blaze3d.platform.InputConstants;

// 1.8.9 -> 1.20.1 input porting note:
//   The original Smart Moving resolved key names/codes via LWJGL2's
//   org.lwjgl.input.Keyboard / org.lwjgl.input.Mouse, reflected inside
//   net.smart.properties.Value. LWJGL2 no longer exists in 1.20.1, which drives input
//   through GLFW via com.mojang.blaze3d.platform.InputConstants.
//
//   This helper preserves the exact contract the Value class relied on:
//     * keyCode >= 0  -> keyboard key
//     * keyCode <  0  -> mouse button, stored with a -100 offset (button = keyCode + 100)
//
//   NOTE: raw GLFW key codes are not identical to the old LWJGL2 codes. Existing config
//   files that store numeric key codes are normalised in the input / key-binding layer;
//   the name <-> code translation itself is faithful to the original behaviour.
public final class KeyNames
{
	private KeyNames()
	{
	}

	public static String toKeyName(Integer keyCode)
	{
		if(keyCode == null)
			return null;

		try
		{
			if(keyCode >= 0)
				return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();

			return InputConstants.Type.MOUSE.getOrCreate(keyCode + 100).getDisplayName().getString();
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public static Integer toKeyCode(String keyName)
	{
		if(keyName == null)
			return null;

		try
		{
			InputConstants.Key key = InputConstants.getKey(keyName);
			if(key.getType() == InputConstants.Type.MOUSE)
				return key.getValue() - 100;
			return key.getValue();
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
