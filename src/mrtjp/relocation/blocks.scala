/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.util.{List => JList}

import codechicken.lib.vec.{BlockCoord, Cuboid6, Vector3}
import mrtjp.core.block.{InstancedBlock, InstancedBlockTile}
import mrtjp.relocation.handler.RelocationMod
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World

import scala.collection.JavaConversions._

class BlockMovingRow extends InstancedBlock("relocation.BlockMovingRow", Material.iron)
{
    setHardness(-1F)
    setStepSound(Block.soundTypeGravel)
    setCreativeTab(null)
    addSingleTile(classOf[TileMovingRow])
}

object TileMovingRow
{
    def setBlockForRow(w:World, r:BlockRow)
    {
        w.setBlock(r.pos.x, r.pos.y, r.pos.z, RelocationMod.blockMovingRow, 0, 3)
    }

    def getBoxFor(w:World, r:BlockRow, progress:Double) =
    {
        val p = r.pos.copy.offset(r.moveDir^1)
        w.getBlock(p.x, p.y, p.z).getCollisionBoundingBoxFromPool(w, p.x, p.y, p.z) match
        {
            case aabb:AxisAlignedBB => new Cuboid6(aabb).sub(new Vector3(r.pos))
                    .add(new Vector3(BlockCoord.sideOffsets(r.moveDir))*progress)
            case _ => Cuboid6.full.copy
        }
    }
}

class TileMovingRow extends InstancedBlockTile
{
    override def update()
    {
        if (!MovementManager2.isMoving(world, x, y, z)) world.setBlockToAir(x, y, z)
        else pushEntities()
    }

    override def getBlock = RelocationMod.blockMovingRow

    override def getBlockBounds =
    {
        val s = MovementManager2.getEnclosedStructure(world, x, y, z)
        if (s != null)
        {
            val r = s.rows.find(_.contains(x, y, z)).get
            TileMovingRow.getBoxFor(world, r, s.progress)
        }
        else Cuboid6.full
    }

    override def getCollisionBounds = getBlockBounds

    def pushEntities()
    {
        val s = MovementManager2.getEnclosedStructure(world, x, y, z)
        if (s != null)
        {
            val r = s.rows.find(_.contains(x, y, z)).get
            val box = TileMovingRow.getBoxFor(world, r, s.progress).add(new Vector3(r.pos))
            val d = new Vector3(BlockCoord.sideOffsets(r.moveDir))*s.speed
            world.getEntitiesWithinAABBExcludingEntity(null, box.toAABB) match
            {
                case list:JList[_] => for (e <- list.asInstanceOf[JList[Entity]])
                {
                    e.moveEntity(0, d.y max 0, 0)
                    e.addVelocity(d.x, d.y, d.z)
                    e.velocityChanged = true
                }
                case _ =>
            }
        }
    }
}