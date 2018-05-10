package mrtjp

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{IWorldEventListener, World}
import net.minecraftforge.common.capabilities.Capability

import scala.language.implicitConversions

object Implicits {
  implicit class WorldExt(self: World) {
    def getTileCap[T](pos: BlockPos, cap: Capability[T]): Option[T] =
      self.getTileEntity(pos) match {
        case t: TileEntity =>
          if (t.hasCapability(cap, null)) Some(t.getCapability(cap, null))
          else None
        case _ => None
      }

    def getBlock(pos: BlockPos): Option[Block] =
      Option(self.getBlockState(pos)).map(_.getBlock)

//    def getBlockMeta(pos: BlockPos): Option[Int] = getBlock(pos).map(_.getMetaFromState(self.getBlockState(pos)))

    def getBlockAndTE(pos: BlockPos): (IBlockState, TileEntity) =
      (self.getBlockState(pos), self.getTileEntity(pos))
  }

  implicit class IWorldEventListenerExt(self: IWorldEventListener) {
    def markBlockRangeForRenderUpdate(p1: BlockPos, p2: BlockPos) =
      self.markBlockRangeForRenderUpdate(p1.getX, p1.getY, p1.getZ, p2.getX, p2.getY, p2.getZ)
  }

  implicit def int2facing(self: Int): EnumFacing = EnumFacing.getFront(self)
  implicit def facing2int(self: EnumFacing): Int = self.getIndex
}
