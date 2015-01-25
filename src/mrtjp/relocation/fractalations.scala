/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{BlockCoord, Vector3}
import mrtjp.core.block.{InstancedBlockTile, InstancedBlock}
import mrtjp.core.world.WorldLib
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.{IBlockAccess, World}

import scala.collection.mutable.{Set => MSet}

class BlockMovingRow extends InstancedBlock("relocation.BlockMovingRow", Material.iron)
{
    setHardness(-1F)
    setStepSound(Block.soundTypeGravel)
    setCreativeTab(null)
    addSingleTile(classOf[TileMovingRow])

    override def createTileEntity(world:World, meta:Int) = new TileMovingRow(None)

    override def setBlockBoundsBasedOnState(world:IBlockAccess, x:Int, y:Int, z:Int)
    {
        Option(world.getTileEntity(x, y, z)).collect
        {
            case te:TileMovingRow if te.parentPos.isDefined =>
                Option(WorldLib.getTileEntity(te.getWorldObj, te.parentPos.get)).map(p => (te, p))
        }.flatten.collect
        {
            case (te, parent:TileMotor) =>
                val pos = new BlockCoord(te).offset(parent.getMoveDir^1)
                val block = world.getBlock(pos.x, pos.y, pos.z)
                if (parent.offset == 0 || block == null) setBlockBounds(.5F, .5F, .5F, .5F, .5F, .5F)
                else
                {
                    val shift = (16-parent.offset).toFloat/16F
                    val sh = new Vector3(new BlockCoord().offset(parent.getMoveDir))*shift
                    setBlockBounds(
                        (block.getBlockBoundsMinX-sh.x).toFloat,
                        (block.getBlockBoundsMinY-sh.y).toFloat,
                        (block.getBlockBoundsMinZ-sh.z).toFloat,
                        (block.getBlockBoundsMaxX-sh.x).toFloat,
                        (block.getBlockBoundsMaxY-sh.y).toFloat,
                        (block.getBlockBoundsMaxZ-sh.z).toFloat)
                }
        }
    }

    override def getCollisionBoundingBoxFromPool(world:World, x:Int, y:Int, z:Int) =
    {
        setBlockBoundsBasedOnState(world, x, y, z)
        Option(world.getTileEntity(x, y, z)).collect
            {
                case te:TileMovingRow if te.parentPos.isDefined =>
                    Option(WorldLib.getTileEntity(te.getWorldObj, te.parentPos.get))
            }.flatten.collect
            {
                case te:TileMotor => te.getAabb(new BlockCoord(x, y, z))
            }.orNull
    }
}

class TileMovingRow(var parentPos:Option[BlockCoord] = None) extends InstancedBlockTile
{
    override def update(){super.update(); checkLocation()}
    override def updateClient(){super.updateClient(); checkLocation()}

    def checkLocation()
    {
        def voidBlock(){world.setBlock(xCoord, yCoord, zCoord, Blocks.air, 0, 3)}
        parentPos match
        {
            case Some(e) => WorldLib.getTileEntity(world, e) match
            {
                case te:TileMotor =>
                case _ => voidBlock()
            }
            case None => voidBlock()
        }
    }

    override def getBlock = RelocationMod.blockMovingRow

    override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        val (x, y, z) = parentPos match
        {
            case Some(bc) => (bc.x, bc.y, bc.z)
            case None => (0, -1, 0)
        }
        tag.setIntArray("pos", Array(x, y, z))
    }

    override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        parentPos = Option(new BlockCoord(tag.getIntArray("pos")))
    }

    override def readDesc(in:MCDataInput)
    {
        super.readDesc(in)
        parentPos = Option(in.readCoord())
    }

    override def writeDesc(out:MCDataOutput)
    {
        super.writeDesc(out)
        val (x, y, z) = parentPos match
        {
            case Some(bc) => (bc.x, bc.y, bc.z)
            case None => (0, -1, 0)
        }
        out.writeCoord(x, y, z)
    }
}

case class BlockRow(pos:BlockCoord, moveDir:Int, size:Int)
{
    def preMove(w:World, offset:RenderPos)
    {
        for (i <- 1 to size)
        {
            val bc = pos.copy.offset(moveDir^1, i)
            MovementManager.addMoving(w, bc, offset)
        }
    }

    def doMove(w:World)
    {
        if (pos.y < 0 || pos.y >= 256) return

        w.removeTileEntity(pos.x, pos.y, pos.z)
        WorldLib.uncheckedSetBlock(w, pos.x, pos.y, pos.z, Blocks.air, 0) //Remove movement block

        for (i <- 1 to size)
        {
            val c = pos.copy.offset(moveDir^1, i)
            MovingTileRegistry.move(w, c.x, c.y, c.z, moveDir)
        }
    }

    def postMove(w:World)
    {
        for (i <- 0 until size)
        {
            val c = pos.copy.offset(moveDir^1, i)
            MovingTileRegistry.postMove(w, c.x, c.y, c.z)
        }
    }

    def endMove(w:World)
    {
        for (i <- 0 to size)
            MovementManager.removeMoving(w, pos.copy.offset(moveDir^1, i))
    }

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