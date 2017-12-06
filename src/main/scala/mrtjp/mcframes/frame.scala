/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import codechicken.lib.vec._
import mrtjp.core.block.BlockCore
import mrtjp.mcframes.api.{IFrame, IFramePlacement, MCFramesAPI}
import mrtjp.relocation.handler.RelocationMod
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, SoundType}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult, Vec3d}
import net.minecraft.util.{EnumActionResult, EnumFacing, EnumHand, ResourceLocation}
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class BlockFrame extends BlockCore(Material.WOOD) with IFrame {
  setResistance(5.0F)
  setHardness(2.0F)
  setSoundType(SoundType.WOOD)
  setCreativeTab(CreativeTabs.TRANSPORTATION)
  setRegistryName(new ResourceLocation(RelocationMod.modID, "frame"))

  override def getItemBlockClass: Class[ItemBlockFrame] = classOf[ItemBlockFrame]

  override def stickOut(w: World, pos: BlockPos, side: EnumFacing) = true
  override def stickIn(w: World, pos: BlockPos, side: EnumFacing) = true

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isNormalCube(state: IBlockState): Boolean = false

  override def collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult =
    MCFramesAPI.instance.raytraceFrame(pos.getX, pos.getY, pos.getZ, 0, start, end)

  @SideOnly(Side.CLIENT)
  override def getSelectedBoundingBox(state: IBlockState, worldIn: World, pos: BlockPos): AxisAlignedBB =
    Cuboid6.full.copy.add(pos).aabb()
}

object ItemBlockFrame {
  var placements: Seq[IFramePlacement] = Seq()
}

class ItemBlockFrame(b: Block) extends ItemBlock(b) {
  override def getMetadata(meta: Int): Int = meta

  override def onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult = {
    if (ItemBlockFrame.placements.exists(_.onItemUse(player.getHeldItem(hand), player, world, pos, hand, facing, new Vector3(hitX, hitY, hitZ)))) {
      // FIXME
      // SoundLib.playBlockPlacement(world, x, y, z, field_150939_a)
      EnumActionResult.SUCCESS
    } else super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ)
  }

  // FIXME: I don't know what this is
  //  override def func_150936_a(world: World, x: Int, y: Int, z: Int, side: Int, player: EntityPlayer, stack: ItemStack) = true
}

//object RenderFrame extends ISimpleBlockRenderingHandler {
//  var renderID = -1
//  var icon: IIcon = _
//
//  private val model = parseModel("frame")
//  private val models = new Array[CCModel](64)
//
//  private def parseModel(name: String) = {
//    val m = combine(parseObjModels(this.getClass.getResource(
//      "/assets/mcframes/obj/" + name + ".obj").openStream(),
//      7, InvertX).values())
//
//    m.apply(new Scale(1.00075, 1.00075, 1.00075))
//    m.apply(new Translation(Vector3.center))
//    m
//  }
//
//  override def getRenderId = renderID
//
//  override def shouldRender3DInInventory(modelId: Int) = true
//
//  override def renderInventoryBlock(b: Block, meta: Int, id: Int, rb: RenderBlocks) {
//    renderInvBlock(rb, meta)
//  }
//
//  override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, b: Block, id: Int, rb: RenderBlocks) = {
//    renderWorldBlock(rb, world, x, y, z, world.getBlockMetadata(x, y, z))
//    true
//  }
//
//  def renderWorldBlock(r: RenderBlocks, w: IBlockAccess, x: Int, y: Int, z: Int, meta: Int) {
//    TextureUtils.bindAtlas(0)
//    CCRenderState.reset()
//    CCRenderState.lightMatrix.locate(w, x, y, z)
//    CCRenderState.setBrightness(w, x, y, z)
//
//    if (r.hasOverrideBlockTexture) {
//      getOrGenerateModel(0).render(new Translation(x, y, z), new IconTransformation(r.overrideBlockTexture))
//    }
//    else RenderFrame.render(new Vector3(x, y, z), 0)
//  }
//
//  def renderInvBlock(r: RenderBlocks, meta: Int) {
//    TextureUtils.bindAtlas(0)
//    CCRenderState.reset()
//    CCRenderState.setDynamic()
//    CCRenderState.pullLightmap()
//
//    CCRenderState.startDrawing()
//    RenderFrame.render(new Vector3(-0.5, -0.5, -0.5), 0)
//
//    CCRenderState.render()
//    CCRenderState.draw()
//  }
//
//  def registerIcons(reg: IIconRegister) {
//    icon = reg.registerIcon("mcframes:frame")
//  }
//
//  def render(pos: Vector3, mask: Int) {
//    getOrGenerateModel(mask).render(pos.translation, new IconTransformation(icon))
//  }
//
//  def getOrGenerateModel(mask: Int) = {
//    var m = models(mask & 0x3F)
//    if (m == null) {
//      m = FrameModelGen.generate(model, mask)
//      models(mask & 0x3F) = m
//    }
//    m
//  }
//}
//
//object FrameModelGen {
//  val w = 2.0 / 16.0
//  val l = 16.0 / 16.0
//  val i = 1.0 / 16.0
//  val u = 0.5
//  val v = 0.5
//
//  def generate(box: CCModel, mask: Int) = {
//    var m = generateSinglePeg
//    m = generateQuartRotated(m)
//    m = generateEightRotated(m)
//    m = generateBackface(m)
//
//    var b = Seq.newBuilder[CCModel]
//    b += box
//    for (s <- 0 until 6) if ((mask & 1 << s) == 0)
//      b += generateSided(m.copy, s)
//
//    finishModel(combine(b.result()))
//  }
//
//  def generateSinglePeg = {
//    val dw = w / 2.0
//    val dl = l / 2.0
//
//    val m = quadModel(4)
//    m.verts(0) = new Vertex5(dw, i, -dl, u + dw, v - dl)
//    m.verts(1) = new Vertex5(dw, i, dl, u + dw, v + dl)
//    m.verts(2) = new Vertex5(-dw, i, dl, u - dw, v + dl)
//    m.verts(3) = new Vertex5(-dw, i, -dl, u - dw, v - dl)
//    m.apply(new Translation(u, 0, v))
//  }
//
//  def generateQuartRotated(m: CCModel) = {
//    combine(Seq(m, m.copy.apply(Rotation.quarterRotations(1)
//      .at(Vector3.center) `with` new Translation(0, 0.01, 0))))
//  }
//
//  def generateEightRotated(m: CCModel) = {
//    m.apply(new Rotation(math.Pi / 4, 0, 1, 0).at(Vector3.center))
//  }
//
//  def generateBackface(m: CCModel) = {
//    combine(Seq(m, m.backfacedCopy()))
//  }
//
//  def generateSided(m: CCModel, side: Int) = {
//    m.apply(Rotation.sideRotations(side).at(Vector3.center.copy))
//  }
//
//  def finishModel(m: CCModel) = {
//    m.shrinkUVs(0.0005)
//    m.computeNormals()
//    m.computeLighting(LightModel.standardLightModel)
//  }
//}