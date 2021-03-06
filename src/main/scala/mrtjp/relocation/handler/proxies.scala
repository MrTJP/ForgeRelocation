/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import codechicken.lib.packet.PacketCustom
import mrtjp.relocation._
import mrtjp.relocation.api.RelocationAPI.{instance => API}
import mrtjp.relocation.handler.RelocationMod.blockMovingRow
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class RelocationProxy_server
{
    def preinit()
    {
        /** Initialization **/
        blockMovingRow = new BlockMovingRow

        /** Registration **/
        ForgeRegistries.BLOCKS.register(blockMovingRow.setRegistryName("blockMovingRow"))

        blockMovingRow.addTile(classOf[TileMovingRow], 0)

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
        API.registerPreferredMover("mod:forgerelocation", "coordpush")
        API.registerPreferredMover("mod:computercraft", "coordpush")
        API.registerPreferredMover("mod:enderstorage", "coordpush")
        API.registerPreferredMover("mod:chickenchunks", "coordpush")
        API.registerPreferredMover("mod:translocator", "coordpush")
        API.registerPreferredMover("mod:projectred-compatibility", "coordpush")
        API.registerPreferredMover("mod:projectred-core", "coordpush")
        API.registerPreferredMover("mod:projectred-expansion", "coordpush")
        API.registerPreferredMover("mod:projectred-exploration", "coordpush")
        API.registerPreferredMover("mod:projectred-fabrication", "coordpush")
        API.registerPreferredMover("mod:projectred-illumination", "coordpush")
        API.registerPreferredMover("mod:projectred-integration", "coordpush")
        API.registerPreferredMover("mod:projectred-transmission", "coordpush")
        API.registerPreferredMover("mod:projectred-transportation", "coordpush")
    }

    def init()
    {
    }

    def postinit()
    {
        PacketCustom.assignHandler(RelocationSPH.channel, RelocationSPH)

        MinecraftForge.EVENT_BUS.register(RelocationEventHandler)
    }
}

class RelocationProxy_client extends RelocationProxy_server
{
    @SideOnly(Side.CLIENT)
    override def init()
    {
        super.init()
        MovingRenderer.init()
    }

    @SideOnly(Side.CLIENT)
    override def postinit()
    {
        super.postinit()
        PacketCustom.assignHandler(RelocationCPH.channel, RelocationCPH)

        MinecraftForge.EVENT_BUS.register(RelocationClientEventHandler)
    }
}

object RelocationProxy extends RelocationProxy_client