/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import codechicken.lib.vec._
import mrtjp.core.block.BlockCore
import mrtjp.mcframes.api.{IFrame, IFramePlacement, MCFramesAPI}
import mrtjp.mcframes.handler.MCFramesMod
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, SoundType}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult, Vec3d}
import net.minecraft.util.{EnumActionResult, EnumFacing, EnumHand, ResourceLocation}
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class BlockFrame extends BlockCore(Material.WOOD) with IFrame {
  setResistance(5.0F)
  setHardness(2.0F)
  setSoundType(SoundType.WOOD)
  setCreativeTab(CreativeTabs.TRANSPORTATION)
  setRegistryName(new ResourceLocation(MCFramesMod.modID, "frame"))
  setUnlocalizedName(s"${MCFramesMod.modID}.frame")

  override def getItemBlockClass: Class[ItemBlockFrame] = classOf[ItemBlockFrame]

  override def stickOut(w: World, pos: BlockPos, side: EnumFacing) = true
  override def stickIn(w: World, pos: BlockPos, side: EnumFacing) = true

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isNormalCube(state: IBlockState): Boolean = false

  // TODO pretty sure this isn't really needed
  // override def collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult =
  //   MCFramesAPI.instance.raytraceFrame(pos.getX, pos.getY, pos.getZ, 0, start, end)

  // TODO this is just the default result, not needed
  // @SideOnly(Side.CLIENT)
  // override def getSelectedBoundingBox(state: IBlockState, worldIn: World, pos: BlockPos): AxisAlignedBB =
  //   Cuboid6.full.copy.add(pos).aabb()
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