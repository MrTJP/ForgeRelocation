/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.util.{List => JList}

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{BlockCoord, Cuboid6, Rotation, Vector3}
import mrtjp.core.block.{InstancedBlock, InstancedBlockTile, TTileOrient}
import mrtjp.core.math.MathLib
import mrtjp.core.world.WorldLib
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.immutable.{HashSet, Queue}
import scala.collection.mutable.{HashMap => MHashMap, MultiMap => MMultiMap, Set => MSet}


class BlockMotor extends InstancedBlock("relocation.blockmotor", Material.iron)
{
    setHardness(5f)
    setResistance(10f)
    setStepSound(Block.soundTypeMetal)
    setCreativeTab(CreativeTabs.tabTransport)
    addSingleTile(classOf[TileMotor])

    override def isOpaqueCube = false

    override def isSideSolid(world:IBlockAccess, x:Int, y:Int, z:Int, side:ForgeDirection) = true

    override def renderAsNormalBlock = true
}

class TileMotor extends InstancedBlockTile with TTileOrient with TPowerTile
{
    var rows = HashSet.empty[BlockRow]
    var isMoving = false
    var offset = 0
    var renderOffset = new RenderPos(0, 0, 0, 6)

    override def onBlockPlaced(side:Int, meta:Int, player:EntityPlayer, stack:ItemStack, hit:Vector3)
    {
        super.onBlockPlaced(side, meta, player, stack, hit)
        setSide(side^1)
        setRotation(Rotation.getSidedRotation(player, side^1))
    }

    override def onBlockActivated(player:EntityPlayer, s:Int):Boolean =
    {
        if (super.onBlockActivated(player, s)) return true

        if (player.isSneaking) setRotation((rotation+1)%4)
        else setSide((side+1)%6)

        true
    }

    override def onOrientChanged(oldOrient:Int)
    {
        super.onOrientChanged(oldOrient)
        sendOrientUpdate()
    }

    override def getBlock = RelocationMod.blockMotor

    override def shouldRefresh(oldB:Block, newB:Block, oldMeta:Int, newMeta:Int, w:World, x:Int, y:Int, z:Int) =
        !(oldB eq newB)

    def getMoveDir = absoluteDir((rotation+2)%4)

    def canMoveAllRows:Boolean =
    {
        if (!world.isBlockIndirectlyGettingPowered(x, y, z)) return false

        val pos = position.offset(side^1)
        val block = WorldLib.getBlock(world, pos)
        if (block == Blocks.air || MovementManager.isMoving(world, pos) ||
            !MovingTileRegistry.canMove(world, pos.x, pos.y, pos.z))
            return false

        val blocks = iterate(Queue(pos))

        val moveEnergy = RelocationConfig.mInitial+RelocationConfig.mLoad*blocks.size
        if (getEnergy < moveEnergy-1/4096F) return false

        val map = new MHashMap[(Int, Int), MSet[Int]] with MMultiMap[(Int, Int), Int]
        for (b <- blocks) map.addBinding(MathLib.normal(b, getMoveDir), MathLib.basis(b, getMoveDir))

        val shift = if ((getMoveDir&1) == 1) 1 else -1
        val rows =
        {
            val builder = Vector.newBuilder[(BlockCoord, Int)]
            for (normal <- map.keys)
            {
                val line = map(normal).toArray
                val sline = if (shift == 1) line.sorted else line.sorted(Ordering[Int].reverse)
                for ((basis, size) <- MathLib.splitLine(sline, shift))
                {
                    val c = MathLib.rhrAxis(getMoveDir, normal, basis+shift)
                    builder += ((c, size))
                }
            }
            builder.result()
        }

        val canMove = (blocks.size <= RelocationConfig.moveLimit) && !rows.exists((pair) =>
        {
            val c = pair._1
            if (c.y < 0 || c.y >= 256) true
            else
            {
                val block = WorldLib.getBlock(world, c)
                if (block == null) false
                else !block.isReplaceable(world, c.x, c.y, c.z)
            }
        })

        if (canMove)
        {
            isMoving = true
            for ((c, size) <- rows)
            {
                world.setBlock(c.x, c.y, c.z, RelocationMod.blockMovingRow, 0, 3)
                WorldLib.getTileEntity(world, c) match
                {
                    case te:TileMovingRow => te.parentPos = Some(position)
                    case _ =>
                }
                world.markBlockForUpdate(c.x, c.y, c.z)
                this.rows += BlockRow(c, getMoveDir, size)
            }
            markDescUpdate()
        }

        if (canMove) setEnergy(getEnergy-moveEnergy)
        canMove
    }

    @tailrec
    private def iterate(open:Seq[BlockCoord], closed:Set[BlockCoord] = Set.empty):Set[BlockCoord] = open match
    {
        case _ if closed.size > RelocationConfig.moveLimit => closed
        case Seq() => closed
        case Seq(next, rest@_*) => WorldLib.getBlock(world, next) match
        {
            case block:Block =>
                val toCheck = Vector.newBuilder[BlockCoord]
                for (s <- 0 until 6)
                {
                    val sticks = MovingTileRegistry.resolveStick(world, next, s)
                    val to = next.copy.offset(s)
                    if (sticks && to != position && !closed(to) && !open.contains(to)
                            && !world.isAirBlock(to.x, to.y, to.z) && !MovementManager.isMoving(world, to)
                            && MovingTileRegistry.canMove(world, to.x, to.y, to.z))
                        toCheck += to
                }
                iterate(rest++toCheck.result(), closed+next)
            case _ => iterate(rest, closed+next)
        }
    }

    def blocks =
    {
        val d = getMoveDir
        for (r <- rows; i <- 1 to r.size) yield r.pos.copy.offset(d^1, i)
    }

    def allBlocks =
    {
        val d = getMoveDir
        for (r <- rows; i <- 0 to r.size) yield r.pos.copy.offset(d^1, i)
    }

    def preMove()
    {
        if (world != null) for (s <- rows) s.preMove(world, renderOffset)
    }

    def cycleMove()
    {
        if (!world.isRemote) writeStream(1).sendToChunk()

        for (r <- rows) r.doMove(world)
        for (r <- rows) r.postMove(world)
        for (r <- rows) r.endMove(world)

        Utils.rescheduleTicks(world, blocks, allBlocks, getMoveDir)

        val changes = MSet[BlockCoord]()
        for (r <- rows) r.cacheChanges(world, changes)
        for (bc <- changes) world.notifyBlockOfNeighborChange(bc.x, bc.y, bc.z, Blocks.air)

        Utils.rerenderBlocks(world, blocks)

        rows = HashSet.empty
        renderOffset.vec.set(0, 0, 0)
        renderOffset.moveDir = 6
    }

    def clientCycleMove()
    {
        cycleMove()
        isMoving = false
        offset = 0
    }

    override def update()
    {
        super.update()
        if (offset >= 16)
        {
            pushEntities()
            cycleMove()
            isMoving = false
            offset = 0
        }
        if (offset == 0 && canMoveAllRows) preMove()
        pushBlock()
    }

    override def updateClient()
    {
        super.updateClient()
        pushBlock()
    }

    def pushBlock()
    {
        if (isMoving && offset <= 16)
        {
            val dir = getMoveDir
            offset += 1
            val shift = offset.toFloat/16.0F
            renderOffset.vec.set(new Vector3(new BlockCoord().offset(dir))*shift)
            renderOffset.moveDir = dir
        }
        pushEntities()
    }

    def pushEntities()
    {
        val sh = new Vector3(new BlockCoord().offset(getMoveDir))*1/16D
        for (s <- rows; aabb = getAabb(s.pos) if aabb != null)
            world.getEntitiesWithinAABBExcludingEntity(null, aabb) match
        {
            case list:JList[_] => for (e <- list.asInstanceOf[JList[Entity]])
            {
                e.moveEntity(0, sh.y max 0, 0)
                e.addVelocity(sh.x, sh.y, sh.z)
            }
            case _ =>
        }
    }

    def getAabb(spos:BlockCoord) =
    {
        val pos = spos.copy.offset(getMoveDir^1)
        val block = world.getBlock(pos.x, pos.y, pos.z)

        if (!isMoving || block == null) null
        else
        {
            val shift = offset.toFloat/16F
            block.getCollisionBoundingBoxFromPool(world, pos.x, pos.y, pos.z) match
            {
                case aabb:AxisAlignedBB => new Cuboid6(aabb).add(new Vector3(new BlockCoord()
                    .offset(getMoveDir))*shift).toAABB
                case _ => null
            }
        }
    }

    def sendOrientUpdate()
    {
        if (!world.isRemote)
            writeStream(2).writeByte(orientation).sendToChunk()
    }

    override def read(in:MCDataInput, key:Int) = key match
    {
        case 1 => clientCycleMove()
        case 2 =>
            orientation = in.readByte()
            markRender()
        case _ => super.read(in, key)
    }

    override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        tag.setBoolean("move", isMoving)
        tag.setShort("off", offset.toShort)

        tag.setInteger("length", rows.size)
        val rowSeq = rows.toSeq
        for (i <- 0 until rows.size)
        {
            val tag2 = new NBTTagCompound
            val s = rowSeq(i)

            tag.setIntArray("pos", Array(s.pos.x, s.pos.y, s.pos.z))
            tag.setByte("dir", getMoveDir.toByte)
            tag.setInteger("size", s.size)

            tag.setTag("tag"+i, tag2)
        }
    }

    override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        isMoving = tag.getBoolean("move")
        offset = tag.getShort("off")

        for (i <- 0 until tag.getInteger("length"))
        {
            val tag2 = tag.getCompoundTag("tag"+i)
            rows += BlockRow(new BlockCoord(tag2.getIntArray("pos")), tag2.getByte("dir"), tag2.getInteger("size"))
        }

        if (isMoving) preMove()
    }

    override def writeDesc(out:MCDataOutput)
    {
        super.writeDesc(out)
        out.writeBoolean(isMoving).writeShort(offset)

        out.writeInt(rows.size)
        val bc = position
        for (row <- rows)
        {
            val delta = row.pos.copy.sub(bc)
            Seq(delta.x, delta.y, delta.z).foreach(out.writeShort)
            out.writeByte(row.moveDir)
            out.writeShort(row.size)
        }
    }

    override def readDesc(in:MCDataInput)
    {
        super.readDesc(in)
        isMoving = in.readBoolean()
        offset = in.readUShort()

        val bc = position
        for (i <- 0 until in.readInt())
        {
            val delta = new BlockCoord(in.readShort(), in.readShort(), in.readShort())
            rows += BlockRow(delta.add(bc), in.readUByte(), in.readUShort())
        }

        if (isMoving) preMove()
    }

    override def compressDesc = true
}