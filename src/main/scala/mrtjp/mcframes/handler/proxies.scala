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
import net.minecraft.item.ItemBlock
import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability.IStorage
import net.minecraftforge.common.capabilities.{Capability, CapabilityManager}
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class MCFramesProxy_server
{
    def preinit()
    {
        blockMotor = new BlockMotor
        blockFrame = new BlockFrame

        /** Localiztion **/
        blockMotor.setUnlocalizedName(s"${MCFramesMod.modID}.motor")
        blockFrame.setUnlocalizedName(s"${MCFramesMod.modID}.frame")

        /** Registration **/
        ForgeRegistries.BLOCKS.register(blockMotor.setRegistryName("motor"))
        ForgeRegistries.ITEMS.register(new ItemBlock(blockMotor).setRegistryName(blockMotor.getRegistryName))
        ForgeRegistries.BLOCKS.register(blockFrame.setRegistryName("frame"))
        ForgeRegistries.ITEMS.register(new ItemBlockFrame(blockFrame).setRegistryName(blockFrame.getRegistryName))

        blockMotor.addTile(classOf[TileMotor], 0)

        ModelLoader.setCustomStateMapper(blockMotor, BlockMotor.StateMapper)
    }

    def init()
    {
        // noinspection ConvertExpressionToSAM
        CapabilityManager.INSTANCE.register(classOf[IFrame], new IStorage[IFrame] {
            override def writeNBT(capability:Capability[IFrame], instance:IFrame, side:EnumFacing):NBTBase = null

            override def readNBT(capability:Capability[IFrame], instance:IFrame, side:EnumFacing, nbt:NBTBase){}
        }, new Callable[IFrame]{
            override def call():IFrame = null
        })
    }

    def postinit() {}
}

class MCFramesProxy_client extends MCFramesProxy_server
{
    @SideOnly(Side.CLIENT)
    override def preinit()
    {
        super.preinit()

        MinecraftForge.EVENT_BUS.register(this)

        FrameRenderer.init()
    }

    @SideOnly(Side.CLIENT)
    override def postinit()
    {
        super.postinit()
    }
}

object MCFramesProxy extends MCFramesProxy_client