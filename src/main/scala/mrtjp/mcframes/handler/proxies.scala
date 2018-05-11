/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.handler

import java.util.concurrent.Callable

import mrtjp.mcframes._
import mrtjp.mcframes.api.IFrame
import mrtjp.mcframes.handler.MCFramesMod._
import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.{Item, ItemBlock}
import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.obj.OBJLoader
import net.minecraftforge.common.capabilities.Capability.IStorage
import net.minecraftforge.common.capabilities.{Capability, CapabilityManager}
import net.minecraftforge.event.RegistryEvent.Register
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class MCFramesProxy_server {
  def preinit() {
    blockMotor = new BlockMotor
    blockFrame = new BlockFrame

    ModelLoader.setCustomStateMapper(blockMotor, BlockMotor.StateMapper)
  }

  def init() {
    // noinspection ConvertExpressionToSAM
    CapabilityManager.INSTANCE.register(classOf[IFrame], new IStorage[IFrame] {
      override def writeNBT(capability: Capability[IFrame], instance: IFrame, side: EnumFacing): NBTBase = null
      override def readNBT(capability: Capability[IFrame], instance: IFrame, side: EnumFacing, nbt: NBTBase): Unit = Unit
    }, new Callable[IFrame] {
      override def call(): IFrame = null
    })
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

    OBJLoader.INSTANCE.addDomain(MCFramesMod.modID)
  }

  @SubscribeEvent
  def registerModels(e: ModelRegistryEvent) {
    ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MCFramesMod.blockMotor), 0,
      new ModelResourceLocation(MCFramesMod.blockMotor.getRegistryName, "inventory"))
    ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(MCFramesMod.blockFrame), 0,
      new ModelResourceLocation(MCFramesMod.blockFrame.getRegistryName, "inventory"))
  }
}