/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.{BlockCoord, Vector3}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.math.MathLib
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntity

object ASMHacks
{
    @SideOnly(Side.CLIENT)
    def renderTileEntityAt(te:TileEntity, x:Double, y:Double, z:Double, tick:Float):Unit =
    {
        if (MovementManager.isMoving(te.getWorldObj, te.xCoord, te.yCoord, te.zCoord))
        {
            val o = MovementManager.getData(te.getWorldObj, te.xCoord, te.yCoord, te.zCoord)
            val vec = o.vec.copy+new Vector3(new BlockCoord().offset(o.moveDir))*tick/16F
            TileEntityRendererDispatcher.instance.renderTileEntityAt(te,
                x+MathLib.clamp(-1F, 1F, vec.x.toFloat),
                y+MathLib.clamp(-1F, 1F, vec.y.toFloat),
                z+MathLib.clamp(-1F, 1F, vec.z.toFloat), tick)
        }
        else TileEntityRendererDispatcher.instance.renderTileEntityAt(te, x, y, z, tick)
    }

    @SideOnly(Side.CLIENT)
    def getRenderType(block:Block, x:Int, y:Int, z:Int):Int =
    {
        if (MovingRenderer.renderHack && MovementManager.isMoving(Minecraft.getMinecraft.theWorld, x, y, z)) -1
        else block.getRenderType
    }
}