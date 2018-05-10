/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.util.{Set => JSet}

import mrtjp.Implicits._
import mrtjp.mcframes.api.{IFrameInteraction, IFramePlacement, MCFramesAPI, StickResolver}
import mrtjp.mcframes.{ItemBlockFrame, StickRegistry}
import mrtjp.relocation.api.RelocationAPI
import net.minecraft.util.math.{BlockPos, Vec3d}
import net.minecraft.world.World

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.immutable.Queue

object MCFramesAPI_Impl extends MCFramesAPI {
  override def registerFramePlacement(placement: IFramePlacement) {
    ItemBlockFrame.placements :+= placement
  }

  override def getFrameBlock = MCFramesMod.blockFrame

  override def registerFrameInteraction(interaction: IFrameInteraction) {
    StickRegistry.interactionList :+= interaction
  }

  override def getStickResolver = StickResolver_Impl

  // FIXME
  // override def renderFrame(x: Double, y: Double, z: Double, mask: Int) {
  //   RenderFrame.render(new Vector3(x, y, z), mask)
  // }

  // FIXME
  override def raytraceFrame(x: Double, y: Double, z: Double, mask: Int, start: Vec3d, end: Vec3d) =
    null
  // ModelRayTracer.raytraceModel(x, y, z, start, end, RenderFrame.getOrGenerateModel(mask))
}

object StickResolver_Impl extends StickResolver {
  private var world: World = _
  private var start: BlockPos = _
  private var excl: Set[BlockPos] = _

  override def getStructure(w: World, pos: BlockPos, ex: BlockPos*): JSet[BlockPos] = {
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
  private def iterate(open: Seq[BlockPos], closed: Set[BlockPos] = Set.empty): Set[BlockPos] = open match {
    case Seq() => closed
    case Seq(next, rest@_*) => world.getBlock(next) match {
      case Some(_) =>
        val toCheck = Vector.newBuilder[BlockPos]
        for (s <- 0 until 6) {
          if (StickRegistry.resolveStick(world, next, s)) {
            val to = next.offset(s)
            if (!excl(to) && !closed(to) && !open.contains(to))
              if (!world.isAirBlock(to) && !RelocationAPI.instance.isMoving(world, to)

              /** && MovingTileRegistry.canMove(world, to.x, to.y, to.z) **/
              ) //Dont ignore non-movables, have them halt movement.
                toCheck += to
          }
        }
        iterate(rest ++ toCheck.result(), closed + next)
      case None => iterate(rest, closed + next)
    }
  }
}