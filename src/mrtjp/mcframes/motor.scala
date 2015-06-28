/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.render.uv.MultiIconTransformation
import codechicken.lib.vec.{Rotation, Vector3}
import mrtjp.core.block.{InstancedBlock, InstancedBlockTile, TTileOrient}
import mrtjp.core.render.TCubeMapRender
import mrtjp.core.world.WorldLib
import mrtjp.mcframes.api.{IFrame, MCFramesAPI}
import mrtjp.mcframes.handler.MCFramesMod
import mrtjp.relocation.api.{BlockPos, RelocationAPI}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class BlockMotor extends InstancedBlock("mcframes.motor", Material.iron)
{
    setHardness(5f)
    setResistance(10f)
    setStepSound(Block.soundTypeMetal)
    setCreativeTab(CreativeTabs.tabTransport)
    addSingleTile(classOf[TileMotor])

    override def isSideSolid(world:IBlockAccess, x:Int, y:Int, z:Int, side:ForgeDirection) = true

    override def renderAsNormalBlock = true
}

class TileMotor extends InstancedBlockTile with TTileOrient with IFrame
{
    override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        tag.setByte("orient", orientation)
    }

    override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        orientation = tag.getByte("orient")
    }

    override def readDesc(in:MCDataInput)
    {
        super.readDesc(in)
        orientation = in.readByte()
    }

    override def writeDesc(out:MCDataOutput)
    {
        super.writeDesc(out)
        out.writeByte(orientation)
    }

    override def read(in:MCDataInput, key:Int) = key match
    {
        case 2 =>
            orientation = in.readByte()
            markRender()
        case _ => super.read(in, key)
    }

    def sendOrientUpdate()
    {
        if (!world.isRemote)
            writeStream(2).writeByte(orientation).sendToChunk()
    }

    override def onBlockPlaced(side:Int, meta:Int, player:EntityPlayer, stack:ItemStack, hit:Vector3)
    {
        super.onBlockPlaced(side, meta, player, stack, hit)
        setSide(side^1)
        setRotation(Rotation.getSidedRotation(player, side^1))
    }

    override def onBlockActivated(player:EntityPlayer, s:Int):Boolean =
    {
        if (super.onBlockActivated(player, s)) return true

        if (player.isSneaking) setRotation((rotation+1)%4)
        else setSide((side+1)%6)

        true
    }

    override def onOrientChanged(oldOrient:Int)
    {
        super.onOrientChanged(oldOrient)
        sendOrientUpdate()
    }

    override def getBlock = MCFramesMod.blockMotor

    def getMoveDir = absoluteDir((rotation+2)%4)

    override def stickOut(w:World, x:Int, y:Int, z:Int, side:Int) = side == (this.side^1)
    override def stickIn(w:World, x:Int, y:Int, z:Int, side:Int) = side != (this.side^1)

    override def update()
    {
        if (!world.isRemote && world.getBlockPowerInput(x, y, z) > 0)
        {
            val pos = position.offset(side^1)
            if (world.isAirBlock(pos.x, pos.y, pos.z)) return

            if (!RelocationAPI.instance.isMoving(world, pos.x, pos.y, pos.z) &&
                    !RelocationAPI.instance.isMoving(world, x, y, z))
            {
                val blocks = MCFramesAPI.instance.getStickResolver
                        .getStructure(world, pos.x, pos.y, pos.z, new BlockPos(x, y, z))

                val r = RelocationAPI.instance.getRelocator
                r.push()
                r.setWorld(world)
                r.setDirection(getMoveDir)
                r.setSpeed(1/16D)
                r.addBlocks(blocks)
                r.execute()
                r.pop()
            }
        }
    }
}

object RenderMotor extends TCubeMapRender
{
    var bottom:IIcon = _
    var side:IIcon = _
    var sidew:IIcon = _
    var sidee:IIcon = _
    var top:IIcon = _

    override def getData(w:IBlockAccess, x:Int, y:Int, z:Int) =
    {
        val te = WorldLib.getTileEntity(w, x, y, z, classOf[TileMotor])
        var s = 0
        var r = 0
        if (te != null)
        {
            s = te.side
            r = te.rotation
        }
        (s, r, new MultiIconTransformation(bottom, top, side, side, sidew, sidee))
    }

    override def getInvData =
        (0, 0, new MultiIconTransformation(bottom, top, side, side, sidew, sidee))

    override def getIcon(s:Int, meta:Int) = s match
    {
        case 0 => bottom
        case 1 => top
        case 2 => side
        case 3 => side
        case 4 => sidew
        case 5 => sidee
    }

    override def registerIcons(reg:IIconRegister)
    {
        bottom = reg.registerIcon("mcframes:motor/bottom")
        top = reg.registerIcon("mcframes:motor/top")
        side = reg.registerIcon("mcframes:motor/side")
        sidew = reg.registerIcon("mcframes:motor/sidew")
        sidee = reg.registerIcon("mcframes:motor/sidee")
    }
}