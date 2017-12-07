/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.lang.{Character => JC}

import mrtjp.mcframes._
import mrtjp.mcframes.handler.MCFramesMod._
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemBlock}
import net.minecraftforge.event.RegistryEvent.Register
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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

  @SubscribeEvent
  def registerBlocks(e: Register[Block]): Unit = {
    e.getRegistry.registerAll(blockMotor, blockFrame)
  }

  @SubscribeEvent
  def registerItems(e: Register[Item]): Unit = {
    e.getRegistry.registerAll(
      new ItemBlock(blockMotor).setRegistryName(blockMotor.getRegistryName),
      new ItemBlockFrame(blockFrame).setRegistryName(blockFrame.getRegistryName)
    )
  }
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