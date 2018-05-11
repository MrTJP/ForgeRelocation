/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.PacketCustom
import mrtjp.core.math.MathLib
import mrtjp.core.world.WorldLib
import mrtjp.relocation.api.{IMovementCallback, IMovementDescriptor}
import mrtjp.relocation.handler.{RelocationConfig, RelocationSPH}
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.mutable.{HashMap => MHashMap, MultiMap => MMultiMap, Set => MSet}
import scala.ref.WeakReference
import mrtjp.Implicits.{facing2int, int2facing}

object MovementManager2 {
  val serverRelocations: MHashMap[Int, WorldStructs] = MHashMap()
  val clientRelocations: MHashMap[Int, WorldStructs] = MHashMap()

  def relocationMap(isClient: Boolean): MHashMap[Int, WorldStructs] =
    if (isClient) clientRelocations else serverRelocations

  def getWorldStructs(w: World): WorldStructs =
    relocationMap(w.isRemote).getOrElseUpdate(w.provider.getDimension, new WorldStructs)

  def getWorld(dim: Int, isClient: Boolean): World =
    if (!isClient) DimensionManager.getWorld(dim)
    else getClientWorld(dim)

  @SideOnly(Side.CLIENT)
  private def getClientWorld(dim: Int): World = {
    val w = Minecraft.getMinecraft.world
    if (w.provider.getDimension == dim) w else null
  }

  def writeDesc(w: World, chunks: Set[ChunkPos], out: MCDataOutput): Boolean = {
    var send = false
    for (s <- getWorldStructs(w).structs if s.getChunks.exists(chunks.contains)) {
      send = true
      out.writeShort(s.id)
      s.writeDesc(out)
    }
    if (send) out.writeShort(Short.MaxValue)
    send
  }

  def readDesc(w: World, in: MCDataInput): Unit = {
    var id = in.readUShort()
    while (id != Short.MaxValue) {
      val struct = new BlockStruct
      struct.id = id
      struct.readDesc(in)
      addStructToWorld(w, struct)
      id = in.readUShort()
    }
  }

  def read(w: World, in: MCDataInput, key: Int): Unit = key match {
    case 1 =>
      val struct = new BlockStruct
      struct.id = in.readUShort()
      struct.readDesc(in)
      addStructToWorld(w, struct)
      for (b <- struct.allBlocks) //rerender all moving blocks
        w.markBlockRangeForRenderUpdate(b, b)
    case 2 =>
      val id = in.readUShort()
      getWorldStructs(w).structs.find(_.id == id) match {
        case Some(struct) => clientCycleMove(w, struct)
        case None => throw new RuntimeException(s"DC: Moving structure with id $id was not found client-side.")
      }
    case _ => throw new RuntimeException(s"DC: Packet with ID $key was not handled. " +
      s"Skipped ${in.asInstanceOf[PacketCustom].array().length} bytes.")
  }

  def sendStruct(w: World, struct: BlockStruct): Unit = {
    val out = RelocationSPH.getStream(w, struct.getChunks, 1)
    out.writeShort(struct.id)
    struct.writeDesc(out)
    RelocationSPH.forceSendData()
  }

  def sendCycle(w: World, struct: BlockStruct): Unit = {
    RelocationSPH.getStream(w, struct.getChunks, 2).writeShort(struct.id)
    RelocationSPH.forceSendData()
  }

  def isMoving(w: World, pos: BlockPos): Boolean = getWorldStructs(w).contains(pos)

  def addStructToWorld(w: World, b: BlockStruct): Unit = {
    getWorldStructs(w).addStruct(b)
    b.onAdded(w)
  }

  def getEnclosedStructure(w: World, pos: BlockPos): BlockStruct =
    getWorldStructs(w).structs.find(_.contains(pos)).orNull

  def tryStartMove(w: World, blocks: Set[BlockPos], moveDir: Int, speed: Double, c: IMovementCallback): Boolean = {
    if (blocks.size > RelocationConfig.moveLimit) return false

    val map = new MHashMap[(Int, Int), MSet[Int]] with MMultiMap[(Int, Int), Int]
    for (b <- blocks) map.addBinding(MathLib.normal(b, moveDir), MathLib.basis(b, moveDir))

    val shift = if ((moveDir & 1) == 1) 1 else -1
    val rowB = Set.newBuilder[BlockRow]
    for (normal <- map.keys) {
      val line = map(normal).toArray
      val sline = if (shift == 1) line.sorted else line.sorted(Ordering[Int].reverse)
      for ((basis, size) <- MathLib.splitLine(sline, shift)) {
        val c = MathLib.rhrAxis(moveDir, normal, basis + shift)
        rowB += new BlockRow(c, moveDir, size)
      }
    }

    val rows = rowB.result()
    if (rows.exists(row => !MovingTileRegistry.canRunOverBlock(w, row.pos))) return false
    for (r <- rows) TileMovingRow.setBlockForRow(w, r)

    val struct = new BlockStruct
    struct.id = BlockStruct.claimID()
    struct.speed = speed
    struct.rows = rows
    struct.callback = WeakReference(c)
    addStructToWorld(w, struct)
    sendStruct(w, struct)

    true
  }

  def onTick(isClient: Boolean) {
    val map = relocationMap(isClient)
    for ((dim, ws) <- map) if (ws.nonEmpty) {
      ws.pushAll()
      val world = getWorld(dim, isClient)
      if (world != null) for (bs <- ws.structs) for (br <- bs.rows)
        br.pushEntities(world, bs.progress)
    }

    if (!isClient) {
      val fin = map.map(pair => (pair._1, pair._2.removeFinished())).filter(_._2.nonEmpty)
      for ((dim, b) <- fin) {
        val w = getWorld(dim, isClient = false)
        if (w != null) for (s <- b) {
          cycleMove(w, s)
          sendCycle(w, s)
        }
      }
    }
  }

  def onWorldUnload(w: World) {
    getWorldStructs(w).clear()
  }

  def clientCycleMove(w: World, struct: BlockStruct) {
    getWorldStructs(w).removeStruct(struct)
    struct.rows.foreach(_.pushEntities(w, 1.0))
    cycleMove(w, struct)
  }

  def cycleMove(w: World, struct: BlockStruct) {
    struct.doMove(w)
    struct.postMove(w)
    struct.endMove(w)

    Utils.rescheduleTicks(w, struct.preMoveBlocks, struct.allBlocks, struct.moveDir)

    val changes = MSet[BlockPos]()
    for (r <- struct.rows) r.cacheChanges(w, changes)
    for (bc <- changes) w.neighborChanged(bc, Blocks.AIR, bc) // FIXME: Update source block?

    Utils.rerenderBlocks(w, struct.preMoveBlocks)
  }
}

class WorldStructs {
  var structs: Set[BlockStruct] = Set.empty

  def isEmpty: Boolean = structs.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def contains(pos: BlockPos): Boolean = structs.exists(_.contains(pos))

  def addStruct(b: BlockStruct): Unit = {
    structs += b
  }

  def pushAll(): Unit = {
    structs.foreach(_.push())
  }

  def removeFinished(): Set[BlockStruct] = {
    val finished = structs.filter(_.isFinished)
    structs = structs.filterNot(_.isFinished)
    finished
  }

  def removeStruct(s: BlockStruct): Unit = {
    structs -= s
  }

  def clear() {
    structs = Set.empty
  }

  def getChunks: Set[ChunkPos] = {
    structs.flatMap(_.getChunks)
  }
}

object BlockStruct {
  private var maxID = 0
  def claimID(): Int = {
    if (maxID < 32765) maxID += 1 //little less than Short.MaxValue (reserved for terminator)
    else maxID = 0
    maxID
  }
}

class MoveDesc(b: WeakReference[BlockStruct]) extends IMovementDescriptor {
  def this(b: BlockStruct) = this(new WeakReference(b))

  override def isMoving: Boolean = b match {
    case WeakReference(b: BlockStruct) => !b.isFinished
    case _ => false
  }

  override def getProgress: Double = b match {
    case WeakReference(b: BlockStruct) => b.progress
    case _ => -1
  }

  override def getSize: Int = b match {
    case WeakReference(b: BlockStruct) => b.allBlocks.size
    case _ => 0
  }
}

class BlockStruct {
  var id: Int = -1
  var speed: Double = 1 / 16D
  var rows: Set[BlockRow] = Set.empty
  var callback: WeakReference[IMovementCallback] = WeakReference(null)

  var progress = 0.0D

  lazy val allBlocks: Set[BlockPos] = rows.flatMap(_.allBlocks)
  lazy val preMoveBlocks: Set[BlockPos] = rows.flatMap(_.preMoveBlocks)
  lazy val postMoveBlocks: Set[BlockPos] = rows.flatMap(_.postMoveBlocks)

  def moveDir: EnumFacing = rows.head.moveDir

  def contains(pos: BlockPos): Boolean = rows.exists(_.contains(pos))

  def push() {
    progress = math.min(1.0, progress + speed)
  }

  def isFinished: Boolean = progress >= 1.0D

  def getChunks: Set[ChunkPos] = {
    val c = Set.newBuilder[ChunkPos]
    for (b <- allBlocks)
      c += new ChunkPos(b.getX >> 4, b.getZ >> 4)
    c.result()
  }

  def onAdded(w: World) {
    if (!w.isRemote) callback match {
      case WeakReference(c) =>
        c.setDescriptor(new MoveDesc(this))
        c.onMovementStarted()
      case _ =>
    }
  }

  def doMove(w: World) {
    for (r <- rows) r.doMove(w)
  }

  def postMove(w: World) {
    for (r <- rows) r.postMove(w)
  }

  def endMove(w: World) {
    for (r <- rows) r.endMove(w)
    if (!w.isRemote) callback match {
      case WeakReference(c) => c.onMovementFinished()
      case _ =>
    }
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: BlockStruct => id == that.id
    case _ => false
  }

  def writeDesc(out: MCDataOutput) {
    out.writeFloat(progress.toFloat)
    out.writeFloat(speed.toFloat)
    out.writeByte(rows.size)
    for (r <- rows) {
      out.writePos(r.pos)
      out.writeByte(r.moveDir)
      out.writeShort(r.size)
    }
  }

  def readDesc(in: MCDataInput) {
    progress = in.readFloat()
    speed = in.readFloat()
    val rb = Set.newBuilder[BlockRow]
    for (_ <- 0 until in.readUByte())
      rb += new BlockRow(in.readPos(), in.readUByte().toInt, in.readShort())
    rows = rb.result()
  }
}

class BlockRow(val pos: BlockPos, val moveDir: EnumFacing, val size: Int) {
  val allBlocks: IndexedSeq[BlockPos] = 0 to size map { i => pos.offset(moveDir.getOpposite, i) }
  val preMoveBlocks: IndexedSeq[BlockPos] = allBlocks drop 1
  val postMoveBlocks: IndexedSeq[BlockPos] = allBlocks dropRight 1

  def contains(pos: BlockPos): Boolean = {
    import MathLib.{basis, normal, shift}

    import math.{max, min}

    if (normal(this.pos, moveDir.getIndex) == normal(pos, moveDir.getIndex)) {
      val b1 = basis(this.pos, moveDir.getIndex)
      val b2 = b1 + size * shift(moveDir.getOpposite.getIndex)
      min(b1, b2) to max(b1, b2) contains basis(pos, moveDir.getOpposite.getIndex)
    }
    else false
  }

  def pushEntities(w: World, progress: Double) {
    WorldLib.uncheckedGetTileEntity(w, pos) match {
      case te: TileMovingRow => te.pushEntities(this, progress)
      case _ =>
    }
  }

  def doMove(w: World) {
    if (pos.getY < 0 || pos.getY >= 256) return

    w.removeTileEntity(pos)
    WorldLib.uncheckedSetBlock(w, pos, Blocks.AIR.getDefaultState) //Remove movement block

    for (b <- preMoveBlocks)
      MovingTileRegistry.move(w, b, moveDir)
  }

  def postMove(w: World): Unit =
    for (b <- postMoveBlocks)
      MovingTileRegistry.postMove(w, b)

  def endMove(w: World) {}

  def cacheChanges(w: World, changes: MSet[BlockPos]) {
    for (i <- 0 to size) {
      val c = pos.offset(moveDir.getOpposite, i)
      changes += c
      for (s <- EnumFacing.VALUES; s1 <- EnumFacing.VALUES if s1 != s.getOpposite)
        changes += c.offset(s).offset(s1)
    }
  }
}