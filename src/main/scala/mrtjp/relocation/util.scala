/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.util.{ArrayList => JAList}

import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world._

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet

object Utils
{
    def rescheduleTicks(world:World, blocks:Set[BlockPos], allBlocks:Set[BlockPos], dir:EnumFacing):Unit = world match {
        case world:WorldServer =>
            val hash = world.pendingTickListEntriesHashSet
            val tree = world.pendingTickListEntriesTreeSet
            val list = world.pendingTickListEntriesThisTick

            val isOptifine = world.getClass.getName == "WorldServerOF"
            val chunks = allBlocks.map(b => world.getChunkFromBlockCoords(b)).filter(_ != null)

            val scheduledTicks = chunks.flatMap(ch => world.getPendingBlockUpdates(ch, !isOptifine)
                    .asInstanceOf[JAList[NextTickListEntry]] match {
                case null => HashSet[NextTickListEntry]()
                case tList => tList.toSet
            })

            if (isOptifine) for (tick <- scheduledTicks) {
                tree.remove(tick)
                hash.remove(tick)
                list.remove(tick)
            }

            for (tick <- scheduledTicks) {
                val bc = tick.position
                if (blocks(bc)) {
                    bc.offset(dir)
                    tick.position = bc
                }
            }

            for (tick <- scheduledTicks) if (!hash.contains(tick)) {
                hash.add(tick)
                tree.add(tick)
            }
        case _ =>
    }

    def rerenderBlocks(world:World, blocks:Set[BlockPos])
    {
        if (world.isRemote) for (b <- blocks) {
            Minecraft.getMinecraft.renderGlobal.markBlockRangeForRenderUpdate(
                b.getX, b.getY, b.getZ,
                b.getX, b.getY, b.getZ
            )
        }
    }
}