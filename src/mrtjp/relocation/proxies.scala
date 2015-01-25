/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.TileRenderRegistry
import mrtjp.relocation.api.RelocationAPI.{instance => API}
import net.minecraftforge.common.MinecraftForge

class RelocationProxy_server
{
    def preinit()
    {
        RelocationMod.blockMotor = new BlockMotor
        RelocationMod.blockFrame = new BlockFrame
        RelocationMod.blockMovingRow = new BlockMovingRow

        API.registerTileMover("saveload",
            "Saves the tile and then reloads it in the next position. Reliable but CPU intensive.",
            new SaveLoadTileHandler)

        API.registerTileMover("coordpush",
            "Physically changes the location of tiles. Works if tiles do not cache their position.",
            new CoordPushTileHandler)

        API.registerTileMover("static", "Setting this disables movement for the specified block.", new StaticTileHandler)

        API.registerPreferredMover("default", "saveload")
        API.registerPreferredMover("mod:minecraft", "coordpush")
        API.registerPreferredMover("mod:Relocation", "coordpush")
        API.registerPreferredMover("mod:ComputerCraft", "coordpush")
        API.registerPreferredMover("mod:EnderStorage", "coordpush")
        API.registerPreferredMover("mod:ChickenChunks", "coordpush")
        API.registerPreferredMover("mod:Translocator", "coordpush")
    }

    def init()
    {
        RelocationConfig.loadConfig()
    }

    def postinit(){}
}

class RelocationProxy_client extends RelocationProxy_server
{
    @SideOnly(Side.CLIENT)
    override def preinit()
    {
        super.preinit()
        FMLCommonHandler.instance.bus.register(RenderTicker)
        MinecraftForge.EVENT_BUS.register(MovingRenderer)

        TileRenderRegistry.setRenderer(RelocationMod.blockMotor, 0, MotorRenderer)
        TileRenderRegistry.setRenderer(RelocationMod.blockFrame, 0, FrameRenderer)
    }

    @SideOnly(Side.CLIENT)
    override def postinit()
    {
        super.postinit()
    }
}

object RelocationProxy extends RelocationProxy_client