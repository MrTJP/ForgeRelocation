/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import mrtjp.core.data.{ModConfig, SpecialConfigGui, TModGuiFactory}
import mrtjp.relocation.api._
import mrtjp.relocation.{BlockMovingRow, MovingTileRegistry, StickRegistry}
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import org.apache.logging.log4j.{LogManager, Logger}

@Mod(modid = RelocationMod.modID, useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.relocation.handler.GuiConfigFactory")
object RelocationMod
{
    RelocationAPI.instance = RelocationAPI_Impl

    final val modID = "forgerelocation"
    final val modName = "ForgeRelocation"
    final val version = "@VERSION@"
    final val buildnumber = "@BUILD_NUMBER@"

    val log:Logger = LogManager.getFormatterLogger(modID)

    var blockMovingRow:BlockMovingRow = _

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        RelocationProxy.preinit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        RelocationAPI_Impl.isPreInit = false
        RelocationConfig.loadConfig()
        RelocationProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        RelocationProxy.postinit()
    }
}

class RelocationConfigGui(parent:GuiScreen) extends SpecialConfigGui(parent, "forgerelocation", RelocationConfig.config)

class GuiConfigFactory extends TModGuiFactory
{
    override def createConfigGui(parentScreen:GuiScreen):GuiScreen = new RelocationConfigGui(parentScreen)
}

object RelocationConfig extends ModConfig("forgerelocation")
{
    var moveLimit = 2048

    var moverMap = Array(
        "default -> saveload"
    )

    var setMap = Array(
        "minecraft:bed -> minecraft:bed",
        "minecraft:wooden_door -> minecraft:wooden_door",
        "minecraft:iron_door -> minecraft:iron_door"
    )

    override protected def initValues()
    {
        val general = BaseCategory("General", "Basic settings")
        moveLimit = general.put("moveLimit", moveLimit, "Maximum amount of blocks that can be moved at once.")

        val movers = BaseCategory("Tile Movers", buildMoverDesc)
        moverMap = movers.put("mover registry", moverMap)
        moverMap = movers.put("mover registry", MovingTileRegistry.parseAndSetMovers(moverMap), force = true)

        val sets = BaseCategory("Latched Sets", buildLatchSetsDesc)
        setMap = sets.put("latch registry", setMap)
        setMap = sets.put("latch registry", StickRegistry.parseAndAddLatchSets(setMap), force = true)

    }

    def buildMoverDesc:String =
    {
        var s =
            """Used to configure which registered Tile Mover is used for a block. Key-Value pairs are defined using
              |the syntax key -> value.
              |Most blocks are configurable, but some mods may have opted to lock which handlers can be used for its
              |blocks.
              |Possible keys:
              |    'default' - to assign default handler.
              |    mod:<modID>' - to assign every block from a mod.
              |    <modID>:<blockname>' - to assign block from a mod for every meta.
              |    <modID>:<blockname>m<meta>' - to assign block from mod for specific meta.
            """.stripMargin

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

    def buildLatchSetsDesc:String =
        """Used to define which pairs of blocks will be stuck together.
          |Latched sets will always move in pairs, even if only one of them are actually connected to a frame.
          |'block1 -> block2' means that if block1 is moved, any block2 connected to it will also move.
          |However, moving block2 does not move block1. To do that, you must also register block2 -> block1.
          |Sets are defined using the syntax of key -> value.
          |Possible keys and values:
          |    '<modID>:<blockname>' - to assign block from a mod for every meta.
          |    '<modID>:<blockname>#<property>=<value>[,<property>=<value>[,…]]' - to assign block from mod with only the given properties matching.
        """.stripMargin
}