/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.util.{Set => JSet}

import codechicken.lib.render.CCRenderState
import codechicken.lib.vec.Vector3
import mrtjp.Implicits._
import mrtjp.core.vec.ModelRayTracer
import mrtjp.mcframes.api.{IFrameInteraction, IFramePlacement, MCFramesAPI, StickResolver}
import mrtjp.mcframes.{FrameRenderer, ItemBlockFrame, StickRegistry}
import mrtjp.relocation.api.RelocationAPI
import net.minecraft.util.math.{BlockPos, Vec3d}
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

    override def renderFrame(ccrs:scala.Any, x:Double, y:Double, z:Double, mask:Int)
    {
        FrameRenderer.render(ccrs.asInstanceOf[CCRenderState], new Vector3(x, y, z), mask)
    }

    override def raytraceFrame(pos:BlockPos, mask:Int, start:Vec3d, end:Vec3d) =
        ModelRayTracer.raytraceModel(pos.getX, pos.getY, pos.getZ, start, end, FrameRenderer.getOrGenerateModel(mask))
}

object StickResolver_Impl extends StickResolver
{
    private var world:World = _
    private var start:BlockPos = _
    private var excl:Set[BlockPos] = _

    override def getStructure(w:World, pos:BlockPos, ex:BlockPos*):JSet[BlockPos] =
    {
        world = w
        start = pos
        excl = ex.toSet
        val result = iterate(Queue(start))
        world = null
        start = null
        excl = null
        result
    }

    @tailrec
    private def iterate(open:Seq[BlockPos], closed:Set[BlockPos] = Set.empty):Set[BlockPos] = open match {
        case Seq() => closed
        case Seq(next, rest@_*) => world.getBlock(next) match {
            case Some(_) =>
                val toCheck = Vector.newBuilder[BlockPos]
                for (s <- 0 until 6) {
                    if (StickRegistry.resolveStick(world, next, s)) {
                        val to = next.offset(s)
                        if (!excl(to) && !closed(to) && !open.contains(to))
                            if (!world.isAirBlock(to) && !RelocationAPI.instance.isMoving(world, to))
                                toCheck += to
                    }
                }
                iterate(rest ++ toCheck.result(), closed + next)
            case None => iterate(rest, closed + next)
        }
    }
}