/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.Rotation
import mrtjp.Implicits._
import mrtjp.core.block._
import mrtjp.mcframes.api.{IFrame, MCFramesAPI}
import mrtjp.mcframes.handler.MCFramesMod
import mrtjp.relocation.api.RelocationAPI
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.{BlockFaceShape, BlockStateContainer, IBlockState}
import net.minecraft.block.{BlockDirectional, SoundType}
import net.minecraft.client.renderer.block.statemap.StateMap
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.capabilities.Capability

class BlockMotor extends MultiTileBlock(Material.IRON) {
  setHardness(5f)
  setResistance(10f)
  setSoundType(SoundType.METAL)
  setCreativeTab(CreativeTabs.TRANSPORTATION)
  setRegistryName(new ResourceLocation(MCFramesMod.modID, "motor"))
  setUnlocalizedName(s"${MCFramesMod.modID}.motor")
  addTile(classOf[TileMotor], 0)

  override def createBlockState(): BlockStateContainer =
    new BlockStateContainer(this, MultiTileBlock.TILE_INDEX, BlockDirectional.FACING, BlockMotor.Rotation)

  override def getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState = super.getActualState(state, world, pos)
    .withProperty(BlockDirectional.FACING, int2facing(world.getTileEntity(pos).asInstanceOf[TileMotor].side))
    .withProperty(BlockMotor.Rotation, world.getTileEntity(pos).asInstanceOf[TileMotor].rotation.asInstanceOf[Integer])

  override def getBlockFaceShape(world: IBlockAccess, state: IBlockState, pos: BlockPos, face: EnumFacing): BlockFaceShape =
    BlockFaceShape.SOLID
}

object BlockMotor {
  val StateMapper: StateMap = new StateMap.Builder().ignore(MultiTileBlock.TILE_INDEX).build()

  val Rotation: PropertyInteger = PropertyInteger.create("rotation", 0, 3)
}

class TileMotor extends MTBlockTile with TTileOrient with IFrame {
  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("orient", orientation)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    orientation = tag.getByte("orient")
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    orientation = in.readByte()
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeByte(orientation)
  }

  override def read(in: MCDataInput, key: Int): Unit = key match {
    case 2 =>
      orientation = in.readByte()
      markRender()
    case _ => super.read(in, key)
  }

  def sendOrientUpdate() {
    if (!world.isRemote)
      writeStream(2).writeByte(orientation).sendToChunk(this)
  }

  override def onBlockPlaced(side: Int, player: EntityPlayer, stack: ItemStack): Unit = {
    super.onBlockPlaced(side, player, stack)
    setSide(side ^ 1)
    setRotation(Rotation.getSidedRotation(player, side ^ 1))
  }

  override def onBlockActivated(player: EntityPlayer, s: Int): Boolean = {
    if (super.onBlockActivated(player, s)) return true

    if (player.isSneaking) setRotation((rotation + 1) % 4)
    else setSide((side + 1) % 6)

    true
  }

  override def onOrientChanged(oldOrient: Int) {
    super.onOrientChanged(oldOrient)
    sendOrientUpdate()
  }

  override def getBlock: BlockMotor = MCFramesMod.blockMotor

  def getMoveDir: Int = absoluteDir((rotation + 2) % 4)

  override def stickOut(w: World, pos: BlockPos, side: EnumFacing): Boolean = side == this.side.getOpposite
  override def stickIn(w: World, pos: BlockPos, side: EnumFacing): Boolean = side != this.side.getOpposite

  override def updateClient(): Unit = updateBoth()

  override def updateServer(): Unit = updateBoth()

  // FIXME: Why is update() final?
  private def updateBoth(): Unit = {
    if (!world.isRemote && world.isBlockIndirectlyGettingPowered(pos) > 0) {
      val pos = this.pos.offset(side.getOpposite)
      if (world.isAirBlock(pos)) return

      if (!RelocationAPI.instance.isMoving(world, pos) &&
        !RelocationAPI.instance.isMoving(world, pos)) {
        val blocks = MCFramesAPI.instance.getStickResolver
          .getStructure(world, pos, pos)

        val r = RelocationAPI.instance.getRelocator
        r.push()
        r.setWorld(world)
        r.setDirection(getMoveDir)
        r.setSpeed(1 / 16D)
        r.addBlocks(blocks)
        r.execute()
        r.pop()
      }
    }
  }

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = capability match {
    case IFrame.CAPABILITY => true
    case _ => super.hasCapability(capability, facing)
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = capability match {
    case IFrame.CAPABILITY => this.asInstanceOf[T]
    case _ => super.getCapability(capability, facing)
  }
}

//object RenderMotor extends TCubeMapRender
//{
//    var bottom:IIcon = _
//    var side:IIcon = _
//    var sidew:IIcon = _
//    var sidee:IIcon = _
//    var top:IIcon = _
//
//    override def getData(w:IBlockAccess, x:Int, y:Int, z:Int) =
//    {
//        val te = WorldLib.getTileEntity(w, x, y, z, classOf[TileMotor])
//        var s = 0
//        var r = 0
//        if (te != null)
//        {
//            s = te.side
//            r = te.rotation
//        }
//        (s, r, new MultiIconTransformation(bottom, top, side, side, sidew, sidee))
//    }
//
//    override def getInvData =
//        (0, 0, new MultiIconTransformation(bottom, top, side, side, sidew, sidee))
//
//    override def getIcon(s:Int, meta:Int) = s match
//    {
//        case 0 => bottom
//        case 1 => top
//        case 2 => side
//        case 3 => side
//        case 4 => sidew
//        case 5 => sidee
//    }
//
//    override def registerIcons(reg:IIconRegister)
//    {
//        bottom = reg.registerIcon("mcframes:motor/bottom")
//        top = reg.registerIcon("mcframes:motor/top")
//        side = reg.registerIcon("mcframes:motor/side")
//        sidew = reg.registerIcon("mcframes:motor/sidew")
//        sidee = reg.registerIcon("mcframes:motor/sidee")
//    }
//}