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

// 1.8.9 -> 1.20.1 PORT NOTE
// The original read the @Mod annotation members modid()/name()/version() through reflection
// (net.minecraftforge.fml.common.Mod). In 1.20.1 the @Mod annotation only carries the mod id
// (SmartRenderMod.MOD_ID); the display name and version are declared in META-INF/mods.toml.
// They are mirrored here as constants so the rest of the code keeps the same accessors.
public class SmartRenderInfo
{
	public static final String ModId = SmartRenderMod.MOD_ID;
	public static final String ModName = "Smart Render";
	public static final String ModVersion = "2.2";
}
