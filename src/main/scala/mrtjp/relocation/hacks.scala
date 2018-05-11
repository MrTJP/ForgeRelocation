/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.Vector3
import mrtjp.core.math.MathLib
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

object ASMHacks {
  @SideOnly(Side.CLIENT)
  def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, unused: Float): Unit = {
    if (MovementManager2.isMoving(te.getWorld, te.getPos)) {
      val s = MovementManager2.getEnclosedStructure(te.getWorld, te.getPos)
      val vec = MovingRenderer.renderPos(s, partialTicks)

      TileEntityRendererDispatcher.instance.render(te,
        x + MathLib.clamp(-1F, 1F, vec.x.toFloat),
        y + MathLib.clamp(-1F, 1F, vec.y.toFloat),
        z + MathLib.clamp(-1F, 1F, vec.z.toFloat), partialTicks, destroyStage, unused)
    }
    else TileEntityRendererDispatcher.instance.render(te, x, y, z, partialTicks, destroyStage, unused)
  }

  def getTERenderPosition(te: TileEntity, x: Double, y: Double, z: Double, partialTicks: Float): Vector3 = {
    val vec = new Vector3(x, y, z)
    if (te.getWorld != null && MovementManager2.isMoving(te.getWorld, te.getPos)) {
      val s = MovementManager2.getEnclosedStructure(te.getWorld, te.getPos)
      val offset = MovingRenderer.renderPos(s, partialTicks)
      vec.add(
        MathLib.clamp(-1F, 1F, offset.x.toFloat),
        MathLib.clamp(-1F, 1F, offset.y.toFloat),
        MathLib.clamp(-1F, 1F, offset.z.toFloat)
      )
    }
    vec
  }

  @SideOnly(Side.CLIENT)
  def getRenderType(state: IBlockState, pos: BlockPos): EnumBlockRenderType =
    if (MovingRenderer.renderHack && MovementManager2.isMoving(Minecraft.getMinecraft.world, pos)) EnumBlockRenderType.INVISIBLE
    else state.getRenderType
}