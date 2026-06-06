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

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

public class SmartMovingInfo
{
	public static final byte StatePacketId = 0;
	public static final byte ConfigInfoPacketId = 1;
	public static final byte ConfigContentPacketId = 2;
	public static final byte ConfigChangePacketId = 3;
	public static final byte SpeedChangePacketId = 4;
	public static final byte HungerChangePacketId = 5;
	public static final byte SoundPacketId = 6;

	// 1.8.9 -> 1.20.1 adaptation:
	//   Original read modid/name/version straight off the @Mod annotation
	//   (Mod.modid()/Mod.name()/Mod.version()). In 1.20.1 the @Mod annotation only
	//   carries value() (the mod id); the human-readable name and version moved to
	//   mods.toml. We keep the same reflection approach for the id and pull the rest
	//   from the loaded mod metadata. ModName is kept as the original literal
	//   "Smart Moving" so the hashCode-derived chat ids below stay identical (1:1).
	private static final Mod Mod = SmartMovingMod.class.getAnnotation(Mod.class);

	public static final String ModId = Mod.value();
	public static final String ModName = "Smart Moving";
	public static final String ModVersion = getModVersion();
	public static final String ModComVersion = SmartMovingMod.MOD_COM_VERSION;

	public static final String ModComMessage = ModName + " uses communication protocol " + ModComVersion;
	public static final String ModComId = ModName.replace(" ", "") + " " + ModComVersion;

	public static final int DefaultChatId = 0;
	public static final int ConfigChatId = ModName.hashCode();
	public static final int SpeedChatId = ModName.hashCode() + 1;

	private static String getModVersion()
	{
		try
		{
			return ModList.get().getModContainerById(ModId)
					.map(container -> container.getModInfo().getVersion().toString())
					.orElse("16.3");
		}
		catch(Throwable t)
		{
			return "16.3";
		}
	}
}
