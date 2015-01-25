/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.{Cuboid6, Vector3}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.{InstancedBlock, ItemBlockCore}
import mrtjp.core.resource.SoundLib
import mrtjp.relocation.api.{IFrame, IFramePlacement}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class BlockFrame extends InstancedBlock("relocation.blockframe", Material.wood) with IFrame
{
    setResistance(5.0F)
    setHardness(2.0F)
    setStepSound(Block.soundTypeWood)
    setCreativeTab(CreativeTabs.tabTransport)

    override def getItemBlockClass = classOf[ItemBlockFrame]

    override def latchSide(w:World, x:Int, y:Int, z:Int, side:Int) = true

    override def isSideSolid(w:IBlockAccess, x:Int, y:Int, z:Int, side:ForgeDirection) = false

    override def collisionRayTrace(world:World, x:Int, y:Int, z:Int, start:Vec3, end:Vec3) =
        Utils.raytraceFrame(x, y, z, start, end)

    @SideOnly(Side.CLIENT)
    override def getSelectedBoundingBoxFromPool(w:World, x:Int, y:Int, z:Int) =
        Cuboid6.full.copy.expand(0.002).add(new Vector3(x, y, z)).toAABB
}

object ItemBlockFrame
{
    var placements = Seq[IFramePlacement]()
}

class ItemBlockFrame(b:Block) extends ItemBlockCore(b)
{
    override def onItemUse(item:ItemStack, player:EntityPlayer, world:World, x:Int, y:Int, z:Int, side:Int, hitX:Float, hitY:Float, hitZ:Float):Boolean =
    {
        if (ItemBlockFrame.placements.exists(_.onItemUse(item, player, world, x, y, z,
            side, new Vector3(hitX, hitY, hitZ))))
        {
            SoundLib.playBlockPlacement(world, x, y, z, field_150939_a)
            true
        }
        else super.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

    override def func_150936_a(world:World, x:Int, y:Int, z:Int, side:Int, player:EntityPlayer, stack:ItemStack) = true
}