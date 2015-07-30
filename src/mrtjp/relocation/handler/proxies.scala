/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.relocation._
import mrtjp.relocation.api.RelocationAPI.{instance => API}
import net.minecraftforge.common.MinecraftForge

class RelocationProxy_server
{
    def preinit()
    {
        RelocationMod.blockMovingRow = new BlockMovingRow

        API.registerTileMover("saveload",
            "Saves the tile and then reloads it in the next position. Reliable but CPU intensive.",
            new SaveLoadTileMover)

        API.registerTileMover("coordpush",
            "Physically changes the location of tiles. Works if tiles do not cache their position.",
            new CoordPushTileMover)

        API.registerTileMover("static", "Setting this disables movement for the specified block.",
            new StaticTileMover)

        API.registerPreferredMover("default", "saveload")
        API.registerPreferredMover("mod:minecraft", "coordpush")
        API.registerPreferredMover("mod:Relocation", "coordpush")
        API.registerPreferredMover("mod:ComputerCraft", "coordpush")
        API.registerPreferredMover("mod:EnderStorage", "coordpush")
        API.registerPreferredMover("mod:ChickenChunks", "coordpush")
        API.registerPreferredMover("mod:Translocator", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Compatibility", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Core", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Expansion", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Exploration", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Fabrication", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Illumination", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Integration", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Transmission", "coordpush")
        API.registerPreferredMover("mod:ProjRed|Transportation", "coordpush")
    }

    def init()
    {
    }

    def postinit()
    {
        PacketCustom.assignHandler(RelocationSPH.channel, RelocationSPH)

        FMLCommonHandler.instance.bus.register(RelocationEventHandler)
        MinecraftForge.EVENT_BUS.register(RelocationEventHandler)
    }
}

class RelocationProxy_client extends RelocationProxy_server
{
    @SideOnly(Side.CLIENT)
    override def postinit()
    {
        super.postinit()
        PacketCustom.assignHandler(RelocationCPH.channel, RelocationCPH)

        FMLCommonHandler.instance.bus.register(RelocationClientEventHandler)
        MinecraftForge.EVENT_BUS.register(RelocationClientEventHandler)

    }
}

object RelocationProxy extends RelocationProxy_client