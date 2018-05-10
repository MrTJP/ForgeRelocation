/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import mrtjp.core.data.{ModConfig, SpecialConfigGui, TModGuiFactory}
import mrtjp.mcframes.api.MCFramesAPI
import mrtjp.mcframes.{BlockFrame, BlockMotor, StickRegistry}
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.{Mod, SidedProxy}

@Mod(modid = MCFramesMod.modID, useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.mcframes.handler.GuiConfigFactory")
object MCFramesMod {
  MCFramesAPI.instance = MCFramesAPI_Impl

  final val modID = "mcframes"
  final val modName = "MCFrames"
  final val version = "@VERSION@"
  final val buildnumber = "@BUILD_NUMBER@"

  var blockFrame: BlockFrame = _
  var blockMotor: BlockMotor = _

  @SidedProxy(clientSide = "mrtjp.mcframes.handler.MCFramesProxy_client", serverSide = "mrtjp.mcframes.handler.MCFramesProxy_server")
  var proxy: MCFramesProxy_server = _

  @Mod.EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    MinecraftForge.EVENT_BUS.register(proxy)
    proxy.preinit()
  }

  @Mod.EventHandler
  def init(event: FMLInitializationEvent) {
    MCFramesConfig.loadConfig()
    proxy.init()
  }

  @Mod.EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    proxy.postinit()
  }
}

class MCFramesConfigGui(parent: GuiScreen) extends SpecialConfigGui(parent, "MCFrames", MCFramesConfig.config)

class GuiConfigFactory extends TModGuiFactory {
  override def createConfigGui(parentScreen: GuiScreen): GuiScreen = new MCFramesConfigGui(parentScreen)
}

object MCFramesConfig extends ModConfig("MCFrames") {
  var setMap = Array(
    "minecraft:bed -> minecraft:bed",
    "minecraft:wooden_door -> minecraft:wooden_door",
    "minecraft:iron_door -> minecraft:iron_door"
  )

  override protected def initValues() {
    val sets = BaseCategory("Latched Sets", buildLatchSetsDesc)
    setMap = sets.put("latch registry", setMap)
    setMap = sets.put("latch registry", StickRegistry.parseAndAddLatchSets(setMap), force = true)
  }

  def buildLatchSetsDesc: String = {
    var s = "Used to define which pairs of blocks will be stuck together. \n"
    s += "Latched sets will always move in pairs, even if only one of them are actually connected to a block. \n"
    s += "'block1 -> block2' means that if block1 is moved, any block2 connected to it will also move. \n"
    s += "However, moving block2 does not move block1. To do that, you must also register block2 -> block1. \n"
    s += "Sets are defined using the syntax of key -> value. \n"
    s += "Possible keys and values:\n"
    s += "    '<modID>:<blockname>' - to assign block from a mod for every meta. \n"
    s += "    '<modID>:<blockname>#<property1>=<value>[,…]' - to assign block from mod with only the given properties matching. \n"
    s
  }
}