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

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// 1.8.9 architecture note:
//   The original Smart Render was a client-only @Mod that, during FMLPreInitialization,
//   either delegated to PlayerAPI (RenderPlayerAPI / ModelPlayerAPI) when present, or
//   registered its own RenderPlayer via SmartRenderContext.registerRenderers(...).
//
// 1.20.1 architecture note:
//   PlayerAPI does not exist for 1.20.1, so the "own renderer" path is the only path.
//   The renderer/model replacement is performed through SmartRenderClient using the
//   modern EntityRenderersEvent hooks (see SmartRenderClient), assisted by Mixins for
//   the parts of HumanoidModel / PlayerModel / PlayerRenderer that must be intercepted.
@Mod(SmartRenderMod.MOD_ID)
public final class SmartRenderMod
{
	public static final String MOD_ID = "smartrender";
	public static final Logger LOGGER = LogUtils.getLogger();

	public SmartRenderMod()
	{
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		// Smart Render is purely client-side. All wiring lives in SmartRenderClient,
		// which is only constructed on the physical client.
		net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SmartRenderClient.init(modEventBus));
	}
}
