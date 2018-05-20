/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import codechicken.lib.lighting.LightModel
import codechicken.lib.model.ModelRegistryHelper
import codechicken.lib.render.CCModel._
import codechicken.lib.render.block.{BlockRenderingRegistry, ICCBlockRenderer}
import codechicken.lib.render.item.IItemRenderer
import codechicken.lib.render.{CCModel, CCRenderState, OBJParser}
import codechicken.lib.texture.TextureUtils.IIconRegister
import codechicken.lib.util.TransformUtils
import codechicken.lib.vec._
import codechicken.lib.vec.uv.IconTransformation
import mrtjp.core.block.BlockCore
import mrtjp.mcframes.api.{IFrame, IFramePlacement, MCFramesAPI}
import mrtjp.mcframes.handler.MCFramesMod
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, SoundType}
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.{TextureAtlasSprite, TextureMap}
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, Vec3d}
import net.minecraft.util.{EnumActionResult, EnumFacing, EnumHand, ResourceLocation}
import net.minecraft.world.{IBlockAccess, World}
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._

class BlockFrame extends BlockCore(Material.WOOD) with IFrame
{
  setResistance(5.0F)
  setHardness(2.0F)
  setSoundType(SoundType.WOOD)
  setCreativeTab(CreativeTabs.TRANSPORTATION)

  override def getItemBlockClass: Class[ItemBlockFrame] = classOf[ItemBlockFrame]

  override def stickOut(w: World, pos: BlockPos, side: EnumFacing) = true
  override def stickIn(w: World, pos: BlockPos, side: EnumFacing) = true

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isNormalCube(state: IBlockState): Boolean = false

    override protected def rayTrace(pos:BlockPos, start:Vec3d, end:Vec3d, boundingBox:AxisAlignedBB) =
        MCFramesAPI.instance.raytraceFrame(pos, 0, start, end)

    override def isSideSolid(base_state:IBlockState, world:IBlockAccess, pos:BlockPos, side:EnumFacing) = false

    override def getRenderType(state:IBlockState) = FrameRenderer.renderType
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

  // this was func_150936_a, not sure if this is the correct method
  override def canPlaceBlockOnSide(worldIn: World, pos: BlockPos, side: EnumFacing, player: EntityPlayer, stack: ItemStack): Boolean =
    true
}

object FrameRenderer extends ICCBlockRenderer with IIconRegister with IItemRenderer
{
    val renderType = BlockRenderingRegistry.createRenderType("mcframes:frame")

    private var icon:TextureAtlasSprite = _
    private var iconT:IconTransformation = _

    private val modelParts = OBJParser.parseModels(new ResourceLocation(
        "mcframes", "models/block/frame.obj"), GL11.GL_QUADS, null).map(a => (a._1, a._2.backfacedCopy))

    private val models = new Array[CCModel](64)

    def init()
    {
        BlockRenderingRegistry.registerRenderer(renderType, this)
        ModelRegistryHelper.registerItemRenderer(Item.getItemFromBlock(MCFramesMod.blockFrame), this)
    }

    override def renderItem(stack:ItemStack, transformType:ItemCameraTransforms.TransformType)
    {
        val ccrs = CCRenderState.instance()
        ccrs.reset()
        ccrs.pullLightmap()
        ccrs.startDrawing(0x07, DefaultVertexFormats.ITEM)
        getOrGenerateModel(0).render(ccrs, iconT)
        ccrs.draw()
    }

    override def getTransforms = TransformUtils.DEFAULT_BLOCK
    override def isAmbientOcclusion = true
    override def isGui3d = true

    override def renderBlock(world:IBlockAccess, pos:BlockPos, state:IBlockState, buffer:BufferBuilder) =
    {
        val ccrs = CCRenderState.instance()
        ccrs.reset()
        ccrs.bind(buffer)
        ccrs.lightMatrix.locate(world, pos)

        ccrs.setBrightness(world, pos)
        render(ccrs, Vector3.fromBlockPos(pos), 0)
        true
    }

    override def handleRenderBlockDamage(world:IBlockAccess, pos:BlockPos, state:IBlockState, sprite:TextureAtlasSprite, buffer:BufferBuilder) = ???

    override def renderBrightness(state:IBlockState, brightness:Float){}
    override def registerTextures(map:TextureMap){}

    override def registerIcons(textureMap:TextureMap)
    {
        icon = textureMap.registerSprite(new ResourceLocation("mcframes:blocks/frame"))

        iconT = new IconTransformation(icon)
    }

    def render(ccrs:CCRenderState, pos:Vector3, mask:Int)
    {
        getOrGenerateModel(mask).render(ccrs, pos.translation, iconT)
    }

    def getOrGenerateModel(mask:Int) =
    {
        var m = models(mask&0x3F)
        if (m == null) {
            m = generateModel(mask)
            models(mask&0x3F) = m
        }
        m
    }

    def generateModel(mask:Int) =
    {
        var m = modelParts("frame")

        for (s <- 0 until 6) if ((mask&1<<s) == 0)
            m = combine(Seq(m, modelParts("cross_"+s)))

        finishModel(m)
    }

    def finishModel(m:CCModel) =
    {
        m.shrinkUVs(0.0005)
        m.computeNormals()
        m.computeLighting(LightModel.standardLightModel)
    }
}

