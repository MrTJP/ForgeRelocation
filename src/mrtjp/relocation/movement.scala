/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.PacketCustom
import codechicken.lib.vec.BlockCoord
import mrtjp.core.math.MathLib
import mrtjp.core.world.WorldLib
import mrtjp.relocation.handler.{RelocationSPH, RelocationConfig}
import net.minecraft.init.Blocks
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.DimensionManager

import scala.collection.mutable.{HashMap => MHashMap, MultiMap => MMultiMap, Set => MSet}

object MovementManager2
{
    val serverRelocations = MHashMap[Int, WorldStructs]()
    val clientRelocations = MHashMap[Int, WorldStructs]()

    def relocationMap(client:Boolean) = if (client) clientRelocations else serverRelocations

    def getWorldStructs(w:World) =
        relocationMap(w.isRemote).getOrElseUpdate(w.provider.dimensionId, new WorldStructs)

    def writeDesc(w:World, chunks:Set[ChunkCoordIntPair], out:MCDataOutput) =
    {
        var send = false
        for (s <- getWorldStructs(w).structs if s.getChunks.exists(chunks.contains))
        {
            send = true
            out.writeShort(s.id)
            s.writeDesc(out)
        }
        if (send) out.writeShort(Short.MaxValue)
        send
    }

    def readDesc(w:World, in:MCDataInput)
    {
        var id = in.readUShort()
        while (id != Short.MaxValue)
        {
            val struct = new BlockStruct
            struct.id = id
            struct.readDesc(in)
            id = in.readUShort()
        }
    }

    def read(w:World, in:MCDataInput, key:Int) = key match
    {
        case 1 =>
            val struct = new BlockStruct
            struct.id = in.readUShort()
            struct.readDesc(in)
            getWorldStructs(w).addStruct(struct)
            for (b <- struct.allBlocks)
                w.func_147479_m(b.x, b.y, b.z)
        case 2 =>
            val id = in.readUShort()
            getWorldStructs(w).structs.find(_.id == id) match
            {
                case Some(struct) => clientCycleMove(w, struct)
                case None => throw new RuntimeException(s"DC: Moving structure with id $id was not found client-side.")
            }
        case _ => throw new RuntimeException(s"DC: Packet with ID $key was not handled. " +
                s"Skipped ${in.asInstanceOf[PacketCustom].getByteBuf.array().length} bytes.")
    }

    def sendStruct(w:World, struct:BlockStruct)
    {
        val out = RelocationSPH.getStream(w, struct.getChunks, 1)
        out.writeShort(struct.id)
        struct.writeDesc(out)
        RelocationSPH.forceSendData()
    }

    def sendCycle(w:World, struct:BlockStruct)
    {
        RelocationSPH.getStream(w, struct.getChunks, 2).writeShort(struct.id)
        RelocationSPH.forceSendData()
    }

    def isMoving(w:World, x:Int, y:Int, z:Int) = getWorldStructs(w).contains(x, y, z)

    def getEnclosedStructure(w:World, x:Int, y:Int, z:Int) =
        getWorldStructs(w).structs.find(_.contains(x, y, z)).orNull

    def tryStartMove(w:World, blocks:Set[BlockCoord], moveDir:Int):Boolean =
    {
        if (blocks.size > RelocationConfig.moveLimit) return false

        val map = new MHashMap[(Int, Int), MSet[Int]] with MMultiMap[(Int, Int), Int]
        for (b <- blocks) map.addBinding(MathLib.normal(b, moveDir), MathLib.basis(b, moveDir))

        val shift = if ((moveDir&1) == 1) 1 else -1
        val rowB = Set.newBuilder[BlockRow]
        for (normal <- map.keys)
        {
            val line = map(normal).toArray
            val sline = if (shift == 1) line.sorted else line.sorted(Ordering[Int].reverse)
            for ((basis, size) <- MathLib.splitLine(sline, shift))
            {
                val c = MathLib.rhrAxis(moveDir, normal, basis+shift)
                rowB += new BlockRow(c, moveDir, size)
            }
        }

        val rows = rowB.result()
        if (rows.exists(row => !MovingTileRegistry.canRunOverBlock(w, row.pos.x, row.pos.y, row.pos.z))) return false
        for (r <- rows) TileMovingRow.setBlockForRow(w, r)

        val struct = new BlockStruct
        struct.id = BlockStruct.claimID()
        struct.rows = rows
        getWorldStructs(w).addStruct(struct)
        sendStruct(w, struct)

        true
    }

    def onTick(client:Boolean)
    {
        val map = relocationMap(client)
        map.values.foreach(_.pushAll())
        if (!client)
        {
            val fin = map.map(pair => (pair._1, pair._2.removeFinished())).filter(_._2.nonEmpty)
            for ((dim, b) <- fin)
            {
                val w = DimensionManager.getWorld(dim)
                if (w != null) for (s <- b)
                {
                    cycleMove(w, s)
                    sendCycle(w, s)
                }
            }
        }
    }

    def onWorldUnload(w:World)
    {
        getWorldStructs(w).clear()
    }

    def clientCycleMove(w:World, struct:BlockStruct)
    {
        getWorldStructs(w).removeStruct(struct)
        cycleMove(w, struct)
    }

    def cycleMove(w:World, struct:BlockStruct)
    {
        for (r <- struct.rows) r.doMove(w)
        for (r <- struct.rows) r.postMove(w)
        for (r <- struct.rows) r.endMove(w)

        Utils.rescheduleTicks(w, struct.preMoveBlocks, struct.allBlocks, struct.moveDir)

        val changes = MSet[BlockCoord]()
        for (r <- struct.rows) r.cacheChanges(w, changes)
        for (bc <- changes) w.notifyBlockOfNeighborChange(bc.x, bc.y, bc.z, Blocks.air)

        Utils.rerenderBlocks(w, struct.preMoveBlocks)
    }
}

class WorldStructs
{
    var structs:Set[BlockStruct] = Set.empty

    def isEmpty = structs.isEmpty

    def contains(x:Int, y:Int, z:Int) = structs.exists(_.contains(x, y, z))

    def addStruct(b:BlockStruct){ structs += b }

    def pushAll(){ structs.foreach(_.push()) }

    def removeFinished() =
    {
        val finished = structs.filter(_.finished)
        structs = structs.filterNot(_.finished)
        finished
    }

    def removeStruct(s:BlockStruct)
    {
        structs = structs.filterNot(_ == s)
    }

    def clear(){structs = Set.empty}

    def getChunks:Set[ChunkCoordIntPair] =
    {
        structs.flatMap(_.getChunks)
    }
}

object BlockStruct
{
    private var maxID = 0
    def claimID() =
    {
        if (maxID < 32765) maxID += 1//little less than Short.MaxValue (reserved for terminator)
        else maxID = 0
        maxID
    }
}

class BlockStruct
{
    var id = -1
    var progress = 0.0D
    var speed = 1/16D
    var rows:Set[BlockRow] = Set.empty

    lazy val allBlocks = rows.flatMap(_.allBlocks)
    lazy val preMoveBlocks = rows.flatMap(_.preMoveBlocks)
    lazy val postMoveBlocks = rows.flatMap(_.postMoveBlocks)

    def moveDir = rows.head.moveDir

    def contains(x:Int, y:Int, z:Int) = rows.exists(_.contains(x, y, z))

    def push(){ progress = (progress+speed) min 1.0D }
    def finished = progress >= 1.0D

    def getChunks:Set[ChunkCoordIntPair] =
    {
        val c = Set.newBuilder[ChunkCoordIntPair]
        for (b <- allBlocks)
            c += new ChunkCoordIntPair(b.x>>4, b.z>>4)
        c.result()
    }

    override def equals(obj:scala.Any) = obj match
    {
        case that:BlockStruct => id == that.id
        case _ => false
    }

    def writeDesc(out:MCDataOutput)
    {
        out.writeFloat(progress.toFloat)
        out.writeFloat(speed.toFloat)
        out.writeByte(rows.size)
        for (r <- rows)
        {
            out.writeLong(WorldLib.packCoords(r.pos))
            out.writeByte(r.moveDir)
            out.writeShort(r.size)
        }
    }

    def readDesc(in:MCDataInput)
    {
        progress = in.readFloat()
        speed = in.readFloat()
        val rb = Set.newBuilder[BlockRow]
        for (i <- 0 until in.readUByte())
            rb += new BlockRow(WorldLib.unpackCoords(in.readLong()), in.readUByte(), in.readUShort())
        rows = rb.result()
    }
}

class BlockRow(val pos:BlockCoord, val moveDir:Int, val size:Int)
{
    val allBlocks = 0 to size map {i => pos.copy.offset(moveDir^1, i)}
    val preMoveBlocks = allBlocks drop 1
    val postMoveBlocks = allBlocks dropRight 1

    def contains(x:Int, y:Int, z:Int) =
    {
        import MathLib.{basis, normal, shift}

        import math.{max, min}
        if (normal(x, y, z, moveDir) == normal(pos, moveDir))
        {
            val b1 = basis(pos, moveDir)
            val b2 = b1+size*shift(moveDir^1)
            min(b1, b2) to max(b1, b2) contains basis(x, y, z, moveDir)
        }
        else false
    }

    def doMove(w:World)
    {
        if (pos.y < 0 || pos.y >= 256) return

        w.removeTileEntity(pos.x, pos.y, pos.z)
        WorldLib.uncheckedSetBlock(w, pos.x, pos.y, pos.z, Blocks.air, 0) //Remove movement block

        for (b <- preMoveBlocks)
            MovingTileRegistry.move(w, b.x, b.y, b.z, moveDir)
    }

    def postMove(w:World)
    {
        for (b <- postMoveBlocks)
            MovingTileRegistry.postMove(w, b.x, b.y, b.z)
    }

    def endMove(w:World){}

    def cacheChanges(w:World, changes:MSet[BlockCoord])
    {
        for (i <- 0 to size)
        {
            val c = pos.copy.offset(moveDir^1, i)
            changes += c
            for (s <- 0 until 6; s1 <- 0 until 6 if s1 != (s^1))
                changes += c.copy.offset(s).offset(s1)
        }
    }
}