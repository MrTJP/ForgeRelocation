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
import mrtjp.mcframes.api.{IFramePlacement, MCFramesAPI}
import mrtjp.mcframes.{FrameRenderer, ItemBlockFrame}
import mrtjp.relocation.StickRegistry
import mrtjp.relocation.api.{IFrameInteraction, RelocationAPI, StickResolver}
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

//    override def getStickResolver = StickResolver_Impl

    override def renderFrame(ccrs:scala.Any, x:Double, y:Double, z:Double, mask:Int)
    {
        FrameRenderer.render(ccrs.asInstanceOf[CCRenderState], new Vector3(x, y, z), mask)
    }

    override def raytraceFrame(pos:BlockPos, mask:Int, start:Vec3d, end:Vec3d) =
        ModelRayTracer.raytraceModel(pos.getX, pos.getY, pos.getZ, start, end, FrameRenderer.getOrGenerateModel(mask))
}
