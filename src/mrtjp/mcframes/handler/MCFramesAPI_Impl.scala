/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.util.{Set => JSet}

import codechicken.lib.vec.{BlockCoord, Vector3}
import mrtjp.core.vec.ModelRayTracer
import mrtjp.core.world.WorldLib
import mrtjp.mcframes.api.{IFrameInteraction, IFramePlacement, MCFramesAPI, StickResolver}
import mrtjp.mcframes.{ItemBlockFrame, RenderFrame, StickRegistry}
import mrtjp.relocation.api.{BlockPos, RelocationAPI}
import net.minecraft.block.Block
import net.minecraft.util.Vec3
import net.minecraft.world.World

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.immutable.Queue

object MCFramesAPI_Impl extends MCFramesAPI
{
    override def registerFramePlacement(placement:IFramePlacement)
    {
        ItemBlockFrame.placements :+= placement
    }

    override def getFrameBlock = MCFramesMod.blockFrame

    override def registerFrameInteraction(interaction:IFrameInteraction)
    {
        StickRegistry.interactionList :+= interaction
    }

    override def getStickResolver = StickResolver_Impl

    override def renderFrame(x:Double, y:Double, z:Double, mask:Int)
    {
        RenderFrame.render(new Vector3(x, y, z), mask)
    }

    override def raytraceFrame(x:Double, y:Double, z:Double, mask:Int, start:Vec3, end:Vec3) =
        ModelRayTracer.raytraceModel(x, y, z, start, end, RenderFrame.getOrGenerateModel(mask))
}

object StickResolver_Impl extends StickResolver
{
    private var world:World = null
    private var start:BlockCoord = null
    private var excl:Set[BlockCoord] = null

    override def getStructure(w:World, x:Int, y:Int, z:Int, ex:BlockPos*):JSet[BlockPos] =
    {
        world = w
        start = new BlockCoord(x, y, z)
        excl = ex.map(b => new BlockCoord(b.x, b.y, b.z)).toSet
        val result = iterate(Queue(start))
        world = null
        start = null
        excl = null
        result.map(b => new BlockPos(b.x, b.y, b.z))
    }

    @tailrec
    private def iterate(open:Seq[BlockCoord], closed:Set[BlockCoord] = Set.empty):Set[BlockCoord] = open match
    {
        case Seq() => closed
        case Seq(next, rest@_*) => WorldLib.getBlock(world, next) match
        {
            case block:Block =>
                val toCheck = Vector.newBuilder[BlockCoord]
                for (s <- 0 until 6)
                {
                    if (StickRegistry.resolveStick(world, next, s))
                    {
                        val to = next.copy.offset(s)
                        if (!excl(to) && !closed(to) && !open.contains(to))
                            if (!world.isAirBlock(to.x, to.y, to.z) && !RelocationAPI.instance.isMoving(world, to.x, to.y, to.z)
                                    /**&& MovingTileRegistry.canMove(world, to.x, to.y, to.z)**/)//Dont ignore non-movables, have them halt movement.
                                toCheck += to
                    }
                }
                iterate(rest++toCheck.result(), closed+next)
            case _ => iterate(rest, closed+next)
        }
    }
}