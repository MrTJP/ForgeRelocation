/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.Vector3
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import mrtjp.core.data.{ModConfig, SpecialConfigGui, TModGuiFactory}
import mrtjp.core.world.WorldLib
import mrtjp.relocation.MovingTileRegistry._
import mrtjp.relocation.api._
import net.minecraft.block.Block
import net.minecraft.client.gui.GuiScreen
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Vec3
import net.minecraft.world.World
import org.apache.logging.log4j.LogManager

@Mod(modid = RelocationMod.modID, useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.relocation.GuiConfigFactory")
object RelocationMod
{
    RelocationAPI.instance = RelocationAPI_Impl

    final val modID = "ForgeRelocation"
    final val modName = "ForgeRelocation"
    final val version = "@VERSION@"
    final val buildnumber = "@BUILD_NUMBER@"

    val log = LogManager.getFormatterLogger(modID)

    var blockMovingRow:BlockMovingRow = _
    var blockMotor:BlockMotor = _
    var blockFrame:Block with IFrame = _

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        RelocationProxy.preinit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        RelocationAPI_Impl.isPreInit = false
        RelocationProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        RelocationProxy.postinit()
    }
}

object RelocationConfig extends ModConfig("ForgeRelocation")
{
    var moveLimit = 2048
    var mCap = 1000.0D
    var mInitial = 40.0D
    var mLoad = 10.0D

    var simpleModel = true

    var moverMap = Array(
        "default -> saveload"
    )

    var setMap = Array(
        "bed -> bed",
        "wooden_door -> wooden_door",
        "iron_door -> iron_door"
    )

    override protected def initValues()
    {
        val general = new BaseCategory("General", "Basic settings")
        moveLimit = general.put("moveLimit", moveLimit, "Maxumum amount of blocks that can be moved at once.")

        val renders = new BaseCategory("Rendering", "Settings related to block rendering")
        simpleModel = renders.put("Simple frame model", simpleModel, "Set to false to use a more complex version of the frame model. Looks better but requires more resources.")

        val movers = new BaseCategory("Tile Movers", buildMoverDesc)
        moverMap = movers.put("mover registry", moverMap)
        moverMap = movers.put("mover registry", parseAndSetMovers(moverMap), true)

        val sets = new BaseCategory("Latched Sets", buildLatchSetsDesc)
        setMap = sets.put("latch registry", setMap)
        parseAndAddLatchSets(setMap)

        val power = new BaseCategory("Power", "Settings that manage the power use of motors. The cost of moving n blocks is initial + n * load")
        mCap = power.put("capacity", mCap, "The total power capacity for a motor.")
        mInitial = power.put("initial", mInitial)
        mLoad = power.put("load", mLoad)
    }

    def buildMoverDesc:String =
    {
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
            s += "    '"+k+"' - "+v+"\n"

        if (mandatoryMovers.nonEmpty)
        {
            s += "\nMovers locked via API:\n"
            for ((k, v) <- MovingTileRegistry.mandatoryMovers)
                s += "    "+k+" -> "+v+"\n"
        }

        s
    }

    def buildLatchSetsDesc:String =
    {
        var s = "Used to define which pairs of blocks will be stuck together. \n"
        s += "Latched sets will always move in pairs, even if only one of them are actually connected to a block. \n"
        s += "'block1 -> block2' means that if block1 is moved, any block2 connected to it will also move. \n"
        s += "However, moving block2 does not move block1. To do that, you must also register block2 -> block1. \n"
        s += "Sets are defined using the syntax of key -> value. \n"
        s += "Possible keys and values:\n"
        s += "    '<modID>:<blockname>' - to assign block from a mod for every meta. \n"
        s += "    '<modID>:<blockname>m<meta>' - to assign block from mod for specific meta. \n"
        s
    }
}

class RelocationConfigGui(parent:GuiScreen) extends SpecialConfigGui(parent, "RelocationTestConfig", RelocationConfig.config)

class GuiConfigFactory extends TModGuiFactory
{
    override def mainConfigGuiClass() = classOf[RelocationConfigGui]
}

object RelocationAPI_Impl extends RelocationAPI
{
    var isPreInit = true

    override def registerTileMover(name:String, desc:String, handler:ITileMover) =
    {
        assert(isPreInit)
        MovingTileRegistry.registerTileMover(name, desc, handler)
    }

    override def registerPreferredMover(key:String, value:String)
    {
        assert(isPreInit)
        MovingTileRegistry.preferredMovers :+= (key, value)
    }

    override def registerMandatoryMover(key:String, value:String)
    {
        assert(isPreInit)
        MovingTileRegistry.mandatoryMovers :+= (key, value)
    }

    override def registerFramePlacement(placement:IFramePlacement)
    {
        ItemBlockFrame.placements :+= placement
    }

    override def registerFrameInteraction(interaction:IFrameInteraction)
    {
        MovingTileRegistry.interactionList :+= interaction
    }

    override def getFrameBlock = RelocationMod.blockFrame

    override def getMotorBlock = RelocationMod.blockMotor

    override def uncheckedRemoveTileEntity(w:World, x:Int, y:Int, z:Int)
    {
        WorldLib.uncheckedRemoveTileEntity(w, x, y, z)
    }

    override def uncheckedSetBlock(w:World, x:Int, y:Int, z:Int, b:Block, meta:Int)
    {
        WorldLib.uncheckedSetBlock(w, x, y, z, b, meta)
    }

    override def uncheckedGetTileEntity(w:World, x:Int, y:Int, z:Int) =
        WorldLib.uncheckedGetTileEntity(w, x, y, z)

    override def uncheckedSetTileEntity(w:World, x:Int, y:Int, z:Int, te:TileEntity)
    {
        WorldLib.uncheckedSetTileEntity(w, x, y, z, te)
    }

    override def renderFrame(x:Double, y:Double, z:Double)
    {
        RenderFrame.render(new Vector3(x, y, z))
    }

    override def raytraceFrame(x:Double, y:Double, z:Double, start:Vec3, end:Vec3) =
        Utils.raytraceFrame(x, y, z, start, end)
}