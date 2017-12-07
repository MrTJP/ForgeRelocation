/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import mrtjp.core.data.{ModConfig, SpecialConfigGui, TModGuiFactory}
import mrtjp.relocation.api._
import mrtjp.relocation.{BlockMovingRow, MovingTileRegistry}
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import org.apache.logging.log4j.{LogManager, Logger}

@Mod(modid = RelocationMod.modID, useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.relocation.handler.GuiConfigFactory")
object RelocationMod {
  RelocationAPI.instance = RelocationAPI_Impl

  final val modID = "forgerelocation"
  final val modName = "ForgeRelocation"
  final val version = "@VERSION@"
  final val buildnumber = "@BUILD_NUMBER@"

  val log: Logger = LogManager.getFormatterLogger(modID)

  var blockMovingRow: BlockMovingRow = _

  @Mod.EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    RelocationProxy.preinit()
  }

  @Mod.EventHandler
  def init(event: FMLInitializationEvent) {
    RelocationAPI_Impl.isPreInit = false
    RelocationConfig.loadConfig()
    RelocationProxy.init()
  }

  @Mod.EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    RelocationProxy.postinit()
  }
}

class RelocationConfigGui(parent: GuiScreen) extends SpecialConfigGui(parent, "ForgeRelocation", RelocationConfig.config)

class GuiConfigFactory extends TModGuiFactory {
  // FIXME: Gui Handler
  //  override def mainConfigGuiClass() = classOf[RelocationConfigGui]
  override def createConfigGui(parentScreen: GuiScreen): GuiScreen = ???
  override def hasConfigGui: Boolean = ???
}

object RelocationConfig extends ModConfig("ForgeRelocation") {
  var moveLimit = 2048

  var moverMap = Array(
    "default -> saveload"
  )

  override protected def initValues() {
    val general = BaseCategory("General", "Basic settings")
    moveLimit = general.put("moveLimit", moveLimit, "Maximum amount of blocks that can be moved at once.")

    val movers = BaseCategory("Tile Movers", buildMoverDesc)
    moverMap = movers.put("mover registry", moverMap)
    moverMap = movers.put("mover registry", MovingTileRegistry.parseAndSetMovers(moverMap), force = true)
  }

  def buildMoverDesc: String = {
    var s = "Used to configure which registered Tile Mover is used for a block. Key-Value pairs are defined using \n" +
      "the syntax key -> value. \n"
    s += "Most blocks are configurable, but some mods may have opted to lock which handlers can be used for its \n" +
      "blocks.\n"
    s += "Possible keys: \n"
    s += "    'default' - to assign default handler. \n"
    s += "    'mod:<modID>' - to assign every block from a mod. \n"
    s += "    '<modID>:<blockname>' - to assign block from a mod for every meta. \n"
    s += "    '<modID>:<blockname>m<meta>' - to assign block from mod for specific meta. \n"

    s += "\nAvailable tile movers:\n"
    for ((k, v) <- MovingTileRegistry.moverDescMap)
      s += "    '" + k + "' - " + v + "\n"

    if (MovingTileRegistry.mandatoryMovers.nonEmpty) {
      s += "\nMovers locked via API:\n"
      for ((k, v) <- MovingTileRegistry.mandatoryMovers)
        s += "    " + k + " -> " + v + "\n"
    }

    s
  }
}