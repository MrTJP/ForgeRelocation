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

class BlockMovingRow extends InstancedBlock("relocation.blockmovingrow", Material.iron)
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

    def getBoxFor(w:World, r:BlockRow, progress:Double):Cuboid6 =
    {
        val p = r.pos.copy.offset(r.moveDir^1)
        val bl = w.getBlock(p.x, p.y, p.z)

        if (bl == RelocationMod.blockMovingRow) return Cuboid6.full.copy()

        bl.getCollisionBoundingBoxFromPool(w, p.x, p.y, p.z) match
        {
            case aabb:AxisAlignedBB => new Cuboid6(aabb).sub(new Vector3(r.pos))
                    .add(new Vector3(BlockCoord.sideOffsets(r.moveDir))*progress)
            case _ => Cuboid6.full.copy
        }
    }
}

class TileMovingRow extends InstancedBlockTile
{
    var prevProg = 0.0

    override def update()
    {
        if (!MovementManager2.isMoving(world, x, y, z)) world.setBlockToAir(x, y, z)
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

    def pushEntities(r:BlockRow, progress:Double)
    {
        val box = Cuboid6.full.copy.add(new Vector3(r.preMoveBlocks.head))
                .add(new Vector3(BlockCoord.sideOffsets(r.moveDir)).multiply(progress)).toAABB

        val dp = (if (progress >= 1.0) progress+0.1 else progress)-prevProg
        val d = new Vector3(BlockCoord.sideOffsets(r.moveDir))*dp
        world.getEntitiesWithinAABBExcludingEntity(null, box) match
        {
            case list:JList[_] =>
                for (e <- list.asInstanceOf[JList[Entity]]) e.moveEntity(d.x, d.y max 0, d.z)
            case _ =>
        }

        prevProg = progress
    }
}