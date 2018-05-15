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
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.biome.Biome
import net.minecraft.world.{EnumSkyBlock, IBlockAccess, World, WorldType}
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._

object MovingRenderer {
  var isRendering = false
  var renderHack = true

  private var oldWorld: World = _
  private var frame = 0.0f
  private var renderBlocks: BlockRendererDispatcher = _
  private var movingWorld: MovingWorld = _

  private def mc = Minecraft.getMinecraft
  private def world = mc.world
  private def tes = Tessellator.getInstance()
  private def vbuf = tes.getBuffer

  private def render(pos: BlockPos, rpos: Vector3) {
    val oldOcclusion = mc.gameSettings.ambientOcclusion
    mc.gameSettings.ambientOcclusion = 0

    val block = world.getBlockState(pos)
    if (block.getBlock.isAir(block, world, pos)) return

    val engine = TileEntityRendererDispatcher.instance.renderEngine
    if (engine != null) engine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
    // mc.entityRenderer.enableLightmap(frame)
    mc.entityRenderer.enableLightmap()

    val light = world.getCombinedLight(pos, block.getLightValue(world, pos))
    val l1 = light % 65536
    val l2 = light / 65536

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1, l2)
    GlStateManager.color(0, 0, 0, 0)
    RenderHelper.disableStandardItemLighting()
    GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    GlStateManager.enableBlend()
//    GlStateManager.disableCull()
    GlStateManager.shadeModel(if (Minecraft.isAmbientOcclusionEnabled) GL_SMOOTH else GL_FLAT)

    for (pass <- 0 to 1) {
      vbuf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)
      vbuf.setTranslation(
        -TileEntityRendererDispatcher.staticPlayerX + MathLib.clamp(-1F, 1F, rpos.x.toFloat),
        -TileEntityRendererDispatcher.staticPlayerY + MathLib.clamp(-1F, 1F, rpos.y.toFloat),
        -TileEntityRendererDispatcher.staticPlayerZ + MathLib.clamp(-1F, 1F, rpos.z.toFloat))
      // vbuf.setColorOpaque(1, 1, 1)
      GlStateManager.color(1f, 1f, 1f, 1f)

      // block.getRenderBlockPass
      // if (block.canRenderInPass(pass)) {
      renderHack = false
      // renderBlocks.renderBlockByRenderType(block, x, y, z)
      renderBlocks.renderBlock(block, pos, movingWorld, vbuf)
      renderHack = true
      // }

      vbuf.setTranslation(0, 0, 0)
      tes.draw()
    }
    RenderHelper.enableStandardItemLighting()
    // mc.entityRenderer.disableLightmap(frame)
    mc.entityRenderer.disableLightmap()
    mc.gameSettings.ambientOcclusion = oldOcclusion
  }

  def onRenderWorldEvent() {
    if (oldWorld != world) {
      oldWorld = world
      renderBlocks = mc.getBlockRendererDispatcher
      movingWorld = new MovingWorld(world)
      // renderBlocks = new MovingRenderBlocks(new MovingWorld(world))
      // renderBlocks.renderAllFaces = true
      // TODO make block renderer not check sides
    }

    for (s <- MovementManager2.getWorldStructs(world).structs) for (b <- s.preMoveBlocks)
      render(b, renderPos(s, frame))
  }

  def onPreRenderTick(time: Float) {
    isRendering = true
    frame = time
  }

  def onPostRenderTick() {
    isRendering = false
  }

  def renderPos(s: BlockStruct, partial: Float) = {
    new Vector3(s.moveDir.getDirectionVec.getX, s.moveDir.getDirectionVec.getY, s.moveDir.getDirectionVec.getZ)
      .multiply(s.progress + s.speed * partial)
  }
}

//@SideOnly(Side.CLIENT)
//class MovingRenderBlocks(w: IBlockAccess) extends RenderBlocks(w) {
//  val eps = 1.0 / 0x10000
//
//  override def renderStandardBlock(block: Block, x: Int, y: Int, z: Int) = {
//    if (MovementManager2.isMoving(Minecraft.getMinecraft.theWorld, x, y, z)) {
//      if (renderMinX == 0.0D) renderMinX += eps
//      if (renderMinY == 0.0D) renderMinY += eps
//      if (renderMinZ == 0.0D) renderMinZ += eps
//      if (renderMaxX == 1.0D) renderMaxX -= eps
//      if (renderMaxY == 1.0D) renderMaxY -= eps
//      if (renderMaxZ == 1.0D) renderMaxZ -= eps
//    }
//    super.renderStandardBlock(block, x, y, z)
//  }
//}

class MovingWorld(val world: World) extends IBlockAccess {
  def computeLightValue(pos: BlockPos, tpe: EnumSkyBlock) = {
    (for (s <- EnumFacing.VALUES; c = pos.offset(s))
      yield world.getLightFor(tpe, c)).max
  }

  override def getCombinedLight(pos: BlockPos, lightValue: Int): Int =
    if (MovementManager2.isMoving(Minecraft.getMinecraft.world, pos)) {
      val l1 = computeLightValue(pos, EnumSkyBlock.SKY)
      val l2 = computeLightValue(pos, EnumSkyBlock.BLOCK)
      l1 << 20 | Seq(l2, lightValue).max << 4
    } else {
      world.getCombinedLight(pos, lightValue)
    }

  override def getTileEntity(pos: BlockPos): TileEntity = world.getTileEntity(pos)
  override def getBlockState(pos: BlockPos): IBlockState = world.getBlockState(pos)
  override def isAirBlock(pos: BlockPos): Boolean = world.isAirBlock(pos)
  override def getBiome(pos: BlockPos): Biome = world.getBiome(pos)
  override def getStrongPower(pos: BlockPos, direction: EnumFacing): Int = world.getStrongPower(pos, direction)
  override def getWorldType: WorldType = world.getWorldType
  override def isSideSolid(pos: BlockPos, side: EnumFacing, _default: Boolean): Boolean = world.isSideSolid(pos, side, _default)
}