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

import net.smart.utilities.*;

// 1.8.9 -> 1.20.1 porting note (this file is otherwise ported verbatim):
//   This is the original reflection name registry. It is pure Java (only Name wrappers),
//   so it compiles unchanged.
//   * The third-party mod-compat names (RopesPlus / Carpenters / MacroMod / ladderKit)
//     simply stay dormant unless those mods are present, exactly as in 1.8.9.
//   * The four Minecraft-internal field names below still carry the 1.8.9 SRG/obf names
//     (e.g. field_147365_f). In the 1.20.1 port these internal accesses are reimplemented
//     idiomatically instead of via raw reflection where their consuming layer is ported:
//       - PlayerControllerMP_currentGameType  -> MultiPlayerGameMode#getPlayerMode()
//       - NetServerHandler_ticksForFloatKick  -> Mixin @Shadow on ServerGamePacketListenerImpl
//       - GuiNewChat_chatMessageList          -> ChatComponent handling in the chat layer
//       - ModifiableAttributeInstance_attributeValue -> AttributeInstance#getValue()
//     The original deobfuscated names are preserved here as the canonical reference; the
//     1.20.1 mapping names are wired in when each consuming layer (options/server/chat) lands.
public class SmartMovingInstall
{
	public final static Name RopesPlusCore = new Name("atomicstryker.ropesplus.common.RopesPlusCore");
	public final static Name ModBlockFence = new Name("net.minecraft.src.modBlockFence", "modBlockFence");
	public final static Name MacroModCore = new Name("net.eq2online.macros.core.MacroModCore");
	public final static Name BlockSturdyLadder = new Name("mods.chupmacabre.ladderKit.sturdyLadders.BlockSturdyLadder");
	public final static Name BlockRopeLadder = new Name("mods.chupmacabre.ladderKit.ropeLadders.BlockRopeLadder");

	public final static Name RopesPlusClient = new Name("atomicstryker.ropesplus.client.RopesPlusClient");
	public final static Name RopesPlusClient_onZipLine = new Name("onZipLine");

	public final static Name CarpentersBlockLadder = new Name("carpentersblocks.block.BlockCarpentersLadder");
	public final static Name CarpentersBlockProperties = new Name("carpentersblocks.util.BlockProperties");
	public final static Name CarpentersTEBaseBlock = new Name("carpentersblocks.tileentity.TEBase");
	public final static Name CarpentersBlockProperties_getMetadata = new Name("getMetadata");

	public final static Name NetServerHandler_ticksForFloatKick = new Name("floatingTickCount", "field_147365_f", "f");
	public final static Name GuiNewChat_chatMessageList = new Name("chatLines", "field_146252_h", "h");
	public final static Name PlayerControllerMP_currentGameType = new Name("currentGameType", "field_78779_k", "k");
	public final static Name ModifiableAttributeInstance_attributeValue = new Name("cachedValue", "field_111139_h", "h");
}