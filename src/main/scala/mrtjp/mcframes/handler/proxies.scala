/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.lang.{Character => JC}

import mrtjp.mcframes._
import mrtjp.mcframes.handler.MCFramesMod._
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class MCFramesProxy_server {
  def preinit() {
    blockMotor = new BlockMotor
    blockFrame = new BlockFrame
  }

  def init() {
    MCFramesRecipes.initRecipes()
  }

  def postinit() {}
}

class MCFramesProxy_client extends MCFramesProxy_server {
  @SideOnly(Side.CLIENT)
  override def preinit() {
    super.preinit()

    // FIXME
    //    TileRenderRegistry.setRenderer(MCFramesMod.blockMotor, 0, RenderMotor)
    //
    //    RenderFrame.renderID = RenderingRegistry.getNextAvailableRenderId
    //    RenderingRegistry.registerBlockHandler(RenderFrame)
  }
}

object MCFramesProxy extends MCFramesProxy_client

object MCFramesRecipes {
  def initRecipes() {
    //Frame
    // FIXME
    //        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockFrame, 8),
    //            "sls","lsl","sls",
    //            's':JC, Items.stick,
    //            'l':JC, "logWood"
    //        ))
  }
}