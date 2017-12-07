package mrtjp

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{IWorldEventListener, World}

import scala.language.implicitConversions

object Implicits {

  implicit class WorldExt(self: World) {
    def getTileEntity[T](pos: BlockPos, clazz: Class[T]): Option[T] =
      self.getTileEntity(pos) match {
        case t: T => Some(t)
        case _ => None
      }

    def getBlock(pos: BlockPos): Option[Block] =
      Option(self.getBlockState(pos)).map(_.getBlock)

    def getBlockMeta(pos: BlockPos): Option[Int] = getBlock(pos).map(_.getMetaFromState(self.getBlockState(pos)))

    def getBlockAndTE(pos: BlockPos): (IBlockState, TileEntity) =
      (self.getBlockState(pos), self.getTileEntity(pos))
  }

  // TODO: Needs to be replaced with just BlockState at some point.
  implicit class IBlockStateExt(self: IBlockState) {
    def blockAndMeta: (Block, Int) = (self.getBlock, self.getBlock.getMetaFromState(self))
  }

  implicit class IWorldEventListenerExt(self: IWorldEventListener) {
    def markBlockRangeForRenderUpdate(p1: BlockPos, p2: BlockPos) =
      self.markBlockRangeForRenderUpdate(p1.getX, p1.getY, p1.getZ, p2.getX, p2.getY, p2.getZ)
  }

  implicit def int2facing(self: Int): EnumFacing = EnumFacing.getFront(self)
  implicit def facing2int(self: EnumFacing): Int = self.getIndex
}
