/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.lang.{Character => JC}

import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.TileRenderRegistry
import mrtjp.mcframes._
import mrtjp.mcframes.handler.MCFramesMod._
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe

class MCFramesProxy_server
{
    def preinit()
    {
        blockMotor = new BlockMotor
        blockFrame = new BlockFrame
    }

    def init()
    {
        MCFramesRecipes.initRecipes()
    }

    def postinit(){}
}

class MCFramesProxy_client extends MCFramesProxy_server
{
    @SideOnly(Side.CLIENT)
    override def preinit()
    {
        super.preinit()

        TileRenderRegistry.setRenderer(MCFramesMod.blockMotor, 0, RenderMotor)

        RenderFrame.renderID = RenderingRegistry.getNextAvailableRenderId
        RenderingRegistry.registerBlockHandler(RenderFrame)
    }
}

object MCFramesProxy extends MCFramesProxy_client

object MCFramesRecipes
{
    def initRecipes()
    {
        //Frame
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockFrame, 8),
            "sls","lsl","sls",
            's':JC, Items.stick,
            'l':JC, "logWood"
        ))
    }
}