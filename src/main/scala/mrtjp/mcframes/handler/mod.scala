/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import mrtjp.mcframes.api.MCFramesAPI
import mrtjp.mcframes.{BlockFrame, BlockMotor}
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}

@Mod(modid = MCFramesMod.modID, useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.mcframes.handler.GuiConfigFactory")
object MCFramesMod
{
    MCFramesAPI.instance = MCFramesAPI_Impl

    final val modID = "mcframes"
    final val modName = "MCFrames"
    final val version = "@VERSION@"
    final val buildnumber = "@BUILD_NUMBER@"

    var blockFrame:BlockFrame = _
    var blockMotor:BlockMotor = _

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        MCFramesProxy.preinit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        MCFramesProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        MCFramesProxy.postinit()
    }
}