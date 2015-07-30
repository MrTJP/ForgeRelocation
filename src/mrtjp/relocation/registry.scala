/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.BlockCoord
import cpw.mods.fml.common.Loader
import mrtjp.core.block.BlockLib
import mrtjp.core.world.WorldLib
import mrtjp.core.world.WorldLib._
import mrtjp.mcframes.api.{IFrame, IFrameInteraction}
import mrtjp.relocation.api.ITileMover
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.collection.immutable.ListMap

object MovingTileRegistry extends ITileMover
{
    val rKeyVal = raw"([^\s]+.+[^\s]+)\s*->\s*([^\s]+.+[^\s]+)".r
    val rName = raw"([^\s]+.+[^\s]+)".r
    val rNameMetaM = raw"([^\s]+.+[^\s]+)m(\d+)".r
    val rMod = raw"mod:([^\s]+.+[^\s]+)".r

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

    def parseAndSetMovers(kv:Seq[String]) =
    {
        var moverMap = ListMap(parseKV(kv):_*)
        for ((k, v) <- preferredMovers) if (!moverMap.contains(k)) moverMap += k -> v
        for (pair <- mandatoryMovers) moverMap += pair
        moverMap.foreach(h => setMover(h._1, h._2))
        moverMap.map(p => p._1+" -> "+p._2).toArray
    }

    def setMover(that:String, m:String)
    {
        if (!moverNameMap.contains(m)) return
        val h = moverNameMap(m)
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

    def canRunOverBlock(w:World, x:Int, y:Int, z:Int) =
    {
        if (w.blockExists(x, y, z))
            w.isAirBlock(x, y, z) || WorldLib.isBlockSoft(w, x, y, z, w.getBlock(x, y, z))
        else false
    }
}

class CoordPushTileMover extends ITileMover
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

class SaveLoadTileMover extends ITileMover
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

class StaticTileMover extends ITileMover
{
    override def canMove(w:World, x:Int, y:Int, z:Int) = false

    override def move(w:World, x:Int, y:Int, z:Int, side:Int){}

    override def postMove(w:World, x:Int, y:Int, z:Int){}
}