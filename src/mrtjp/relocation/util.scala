/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.util.{ArrayList => JAList, List => JList, Set => JSet, TreeSet => JTreeSet}

import codechicken.lib.vec.BlockCoord
import cpw.mods.fml.common.ObfuscationReflectionHelper
import mrtjp.core.vec.ModelRayTracer
import mrtjp.mcframes.RenderFrame
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Vec3
import net.minecraft.world._

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet

object Utils
{
    def rescheduleTicks(world:World, blocks:Set[BlockCoord], allBlocks:Set[BlockCoord], dir:Int) = world match
    {
        case world:WorldServer =>
            val hash = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_73064_N",
                "pendingTickListEntriesHashSet").asInstanceOf[JSet[NextTickListEntry]]

            val tree = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_73065_O",
                "pendingTickListEntriesTreeSet").asInstanceOf[JTreeSet[NextTickListEntry]]

            val list = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_94579_S",
                "pendingTickListEntriesThisTick").asInstanceOf[JAList[NextTickListEntry]]

            val isOptifine = world.getClass.getName == "WorldServerOF"

            val chunks = allBlocks.map(b => world.getChunkFromBlockCoords(b.x, b.z)).filter(_ != null)

            val scheduledTicks = chunks.flatMap(ch => world.getPendingBlockUpdates(ch, !isOptifine)
                .asInstanceOf[JAList[NextTickListEntry]] match
            {
                case null => HashSet[NextTickListEntry]()
                case tList => tList.toSet
            })

            if (isOptifine) for (tick <- scheduledTicks)
            {
                tree.remove(tick)
                hash.remove(tick)
                list.remove(tick)
            }

            for (tick <- scheduledTicks)
            {
                val bc = new BlockCoord(tick.xCoord, tick.yCoord, tick.zCoord)
                if (blocks(bc))
                {
                    bc.offset(dir)
                    tick.xCoord = bc.x
                    tick.yCoord = bc.y
                    tick.zCoord = bc.z
                }
            }

            for (tick <- scheduledTicks) if (!hash.contains(tick))
            {
                hash.add(tick)
                tree.add(tick)
            }
        case _ =>
    }

    def rerenderBlocks(world:World, blocks:Set[BlockCoord])
    {
        if (world.isRemote)
        {
            val mc = Minecraft.getMinecraft
            val teList = mc.renderGlobal.tileEntities.asInstanceOf[JList[TileEntity]]

            for (pass <- 0 to 1)
            {
                for(c <- blocks; te = world.getTileEntity(c.x, c.y, c.z))
                {
                    if (te != null) pass match
                    {
                        case 0 => teList.remove(te)
                        case 1 => teList.add(te)
                    }
                    mc.renderGlobal.markBlockForRenderUpdate(c.x, c.y, c.z)
                }
                mc.renderGlobal.updateRenderers(mc.renderViewEntity, false)
            }
        }
    }
}