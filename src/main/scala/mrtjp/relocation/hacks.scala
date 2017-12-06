/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object ASMHacks {
  @SideOnly(Side.CLIENT)
  def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, tick: Float): Unit = {
    // FIXME: render
    //    if (MovementManager2.isMoving(te.getWorld, te.getPos)) {
    //      val s = MovementManager2.getEnclosedStructure(te.getWorld, te.getPos)
    //      val vec = MovingRenderer.renderPos(s, tick)
    //
    //      TileEntityRendererDispatcher.instance.render(te,
    //        x + MathLib.clamp(-1F, 1F, vec.x.toFloat),
    //        y + MathLib.clamp(-1F, 1F, vec.y.toFloat),
    //        z + MathLib.clamp(-1F, 1F, vec.z.toFloat), tick)
    //    }
    //    else TileEntityRendererDispatcher.instance.render(te, x, y, z, tick)
  }

  //  @SideOnly(Side.CLIENT)
  //  def getRenderType(block: Block, pos: BlockPos): Int = {
  // FIXME: render
  //    if (MovingRenderer.renderHack && MovementManager2.isMoving(Minecraft.getMinecraft.world, pos)) -1
  //    else block.getRenderType
  //  }
}