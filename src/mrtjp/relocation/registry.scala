/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.security.InvalidKeyException

import codechicken.lib.vec.BlockCoord
import cpw.mods.fml.common.Loader
import gnu.trove.impl.sync.{TSynchronizedIntObjectMap, TSynchronizedLongObjectMap, TSynchronizedLongSet}
import gnu.trove.map.hash.{TIntObjectHashMap, TLongObjectHashMap}
import gnu.trove.set.hash.TLongHashSet
import mrtjp.core.block.BlockLib
import mrtjp.core.world.WorldLib._
import mrtjp.relocation.api.{IFrame, IFrameInteraction, ITileMover}
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.collection.immutable.ListMap

object MovingTileRegistry extends ITileMover
{
    val rKeyVal = raw"([\w:]+)\s*->\s*(.+)".r
    val rName = raw"(.+)".r
    val rNameMetaM = raw"(.+)m(\d+)".r
    val rMod = raw"mod:(\w+)".r

    var latchMap = Map[(Block, Int), Set[(Block, Int)]]().withDefaultValue(Set())
    var interactionList = Seq[IFrameInteraction]()

    var blockMetaMap = Map[(Block, Int), ITileMover]()
    var modMap = Map[String, ITileMover]()

    var moverDescMap = Map[String, String]()
    var moverNameMap = Map[String, ITileMover]()

    var defaultMover:ITileMover = _
    var preferredMovers = Seq[(String, String)]()
    var mandatoryMovers = Seq[(String, String)]()

    def parseKV(kv:Seq[String]) = kv.map { case rKeyVal(k, v) => (k, v); case s => throw new MatchError(s"Illegal [k -> v] pair: $s") }
    def parseBlockMeta(b:String) = b match
    {
        case rNameMetaM(name, meta) => Block.getBlockFromName(fixName(name)) -> meta.toInt
        case rName(name) => Block.getBlockFromName(fixName(name)) -> -1
        case _ => throw new MatchError(s"Illegal set part: $b")
    }
    def fixName(name:String) = name.indexOf(':') match
    {
        case -1 => "minecraft:"+name
        case i => name
    }

    def parseAndAddLatchSets(kv:Seq[String])
    {
        parseKV(kv).foreach(b => addLatchSet(parseBlockMeta(b._1), parseBlockMeta(b._2)))
    }

    def parseAndSetMovers(kv:Seq[String]) =
    {
        var moverMap = ListMap(parseKV(kv):_*)
        for ((k, v) <- preferredMovers) if (!moverMap.contains(k)) moverMap += k -> v
        for (pair <- mandatoryMovers) moverMap += pair
        moverMap.foreach(h => setMover(h._1, h._2))
        moverMap.map(p => p._1+" -> "+p._2).toArray
    }

    def addLatchSet(b1:(Block, Int), b2:(Block, Int))
    {
        latchMap += b1 -> (latchMap(b1)+b2)
    }

    def setMover(that:String, m:String)
    {
        val h = moverNameMap.getOrElse(m, throw new InvalidKeyException("[ForgeRelocation] No tile mover named '"+m+"'"))
        that match
        {
            case "default" => defaultMover = h
            case rMod(mod) if Loader.isModLoaded(mod) => modMap += mod -> h
            case _ => blockMetaMap += parseBlockMeta(that) -> h
        }
    }

    def registerTileMover(name:String, desc:String, m:ITileMover)
    {
        moverDescMap += name -> desc
        moverNameMap += name -> m
    }

    private def getHandler(b:Block, m:Int) =
    {
        blockMetaMap.getOrElse((b, m), blockMetaMap.getOrElse((b, -1),
            modMap.getOrElse(BlockLib.getModId(b), defaultMover)))
    }

    override def canMove(w:World, x:Int, y:Int, z:Int) =
    {
        val meta = w.getBlockMetadata(x, y, z)
        w.getBlock(x, y, z) match
        {
            case block:Block => getHandler(block, meta).canMove(w, x, y, z)
            case _ => false
        }
    }

    override def move(w:World, x:Int, y:Int, z:Int, side:Int)
    {
        val meta = w.getBlockMetadata(x, y, z)
        w.getBlock(x, y, z) match
        {
            case block:Block => getHandler(block, meta).move(w, x, y, z, side)
            case _ =>
        }
    }

    override def postMove(w:World, x:Int, y:Int, z:Int)
    {
        val meta = w.getBlockMetadata(x, y, z)
        w.getBlock(x, y, z) match
        {
            case block:Block => getHandler(block, meta).postMove(w, x, y, z)
            case _ =>
        }
    }

    def resolveStick(w:World, pos:BlockCoord, side:Int):Boolean =
    {
        val block = getBlock(w, pos)
        if (block.isInstanceOf[IFrame] && block.asInstanceOf[IFrame].
                latchSide(w, pos.x, pos.y, pos.z, side)) return true

        val te = getTileEntity(w, pos)
        if (te.isInstanceOf[IFrame] && te.asInstanceOf[IFrame].
                latchSide(w, pos.x, pos.y, pos.z, side)) return true

        if (latchInteraction(w, pos.x, pos.y, pos.z, side)) return true

        if (latchSet(w, pos.x, pos.y, pos.z, side)) return true

        false
    }

    def latchSet(w:World, x:Int, y:Int, z:Int, side:Int) =
    {
        val pos = new BlockCoord(x, y, z).offset(side)
        val b1 = getBlockMetaPair(w, x, y, z)
        val b2 = getBlockMetaPair(w, pos.x, pos.y, pos.z)

        val set = latchMap.getOrElse(b1, latchMap((b1._1, -1)))
        set.contains(b2) || set.contains((b2._1, -1))
    }

    def latchInteraction(w:World, x:Int, y:Int, z:Int, side:Int) = interactionList.find(_.canInteract(w, x, y, z)) match
    {
        case Some(i) => i.latchSide(w, x, y, z, side)
        case None => false
    }
}

object MovementManager
{
    val eps = 1.0/0x10000

    val clientMap = new TSynchronizedLongObjectMap(new TLongObjectHashMap[RenderPos])
    val serverMap = new TSynchronizedIntObjectMap(new TIntObjectHashMap[TSynchronizedLongSet])

    def isMoving(w:World, bc:BlockCoord):Boolean =
        isMoving(w, bc.x, bc.y, bc.z)

    def isMoving(w:World, x:Int, y:Int, z:Int):Boolean =
    {
        if (w.isRemote) clientMap.containsKey(packCoords(x, y, z))
        else
        {
            val dim = w.provider.dimensionId
            if (serverMap.containsKey(dim))
            {
                val worldSet = serverMap.get(dim)
                worldSet.contains(packCoords(x, y, z))
            }
            else false
        }
    }

    def getData(w:World, bc:BlockCoord):RenderPos =
        getData(w, bc.x, bc.y, bc.z)

    def getData(w:World, x:Int, y:Int, z:Int):RenderPos =
    {
        if (w.isRemote) clientMap.get(packCoords(x, y, z))
        else null
    }

    def addMoving(w:World, bc:BlockCoord, offset:RenderPos) = this.synchronized
    {
        if (w.isRemote)
        {
            clientMap.put(packCoords(bc.x, bc.y, bc.z), offset)
            w.func_147451_t(bc.x, bc.y, bc.z)
            for (s <- 0 until 6)
            {
                val pos = bc.copy.offset(s)
                w.func_147479_m(pos.x, pos.y, pos.z)
            }
        }
        else
        {
            val dim = w.provider.dimensionId
            if (!serverMap.containsKey(dim))
                serverMap.put(dim, new TSynchronizedLongSet(new TLongHashSet))

            val worldSet = serverMap.get(dim)
            worldSet.add(packCoords(bc.x, bc.y, bc.z))
        }
    }

    def removeMoving(w:World, bc:BlockCoord) = this.synchronized
    {
        if (w.isRemote)
        {
            clientMap.remove(packCoords(bc.x, bc.y, bc.z))
            w.func_147451_t(bc.x, bc.y, bc.z)
            for (s <- 0 until 6)
            {
                val pos = bc.copy.offset(s)
                w.func_147479_m(pos.x, pos.y, pos.z)
            }
        }
        else
        {
            val dim = w.provider.dimensionId
            if (serverMap.containsKey(dim))
            {
                val worldSet = serverMap.get(dim)
                worldSet.remove(packCoords(bc.x, bc.y, bc.z))
            }
        }
    }
}

class CoordPushTileHandler extends ITileMover
{
    override def canMove(w:World, x:Int, y:Int, z:Int) = true

    override def move(w:World, x:Int, y:Int, z:Int, side:Int)
    {
        val (b, meta, te) = getBlockInfo(w, x, y, z)
        val pos = new BlockCoord(x, y, z).offset(side)
        if (te != null)
        {
            te.invalidate()
            uncheckedRemoveTileEntity(w, x, y, z)
        }
        uncheckedSetBlock(w, x, y, z, Blocks.air, 0)
        uncheckedSetBlock(w, pos.x, pos.y, pos.z, b, meta)
        if (te != null)
        {
            te.xCoord = pos.x
            te.yCoord = pos.y
            te.zCoord = pos.z
            te.validate()
            uncheckedSetTileEntity(w, pos.x, pos.y, pos.z, te)
        }
    }

    override def postMove(w:World, x:Int, y:Int, z:Int){}
}

class SaveLoadTileHandler extends ITileMover
{
    override def canMove(w:World, x:Int, y:Int, z:Int) = true

    override def move(w:World, x:Int, y:Int, z:Int, side:Int)
    {
        val (b, meta, te) = getBlockInfo(w, x, y, z)
        val pos = new BlockCoord(x, y, z).offset(side)
        val tag = if (te != null)
        {
            val tag = new NBTTagCompound
            te.writeToNBT(tag)
            tag.setInteger("x", pos.x)
            tag.setInteger("y", pos.y)
            tag.setInteger("z", pos.z)
            te.onChunkUnload()
            w.removeTileEntity(x, y, z)
            tag
        }
        else null
        uncheckedSetBlock(w, x, y, z, Blocks.air, 0)
        uncheckedSetBlock(w, pos.x, pos.y, pos.z, b, meta)
        if (tag != null)
        {
            TileEntity.createAndLoadEntity(tag) match
            {
                case te:TileEntity =>
                    w.getChunkFromBlockCoords(pos.x, pos.z).addTileEntity(te)
                case _ =>
            }
        }
    }

    override def postMove(w:World, x:Int, y:Int, z:Int){}
}

class StaticTileHandler extends ITileMover
{
    override def canMove(w:World, x:Int, y:Int, z:Int) = false

    override def move(w:World, x:Int, y:Int, z:Int, side:Int){}

    override def postMove(w:World, x:Int, y:Int, z:Int){}
}