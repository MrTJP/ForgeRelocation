/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import java.util.{ArrayList => JAList, List => JList, Set => JSet, TreeSet => JTreeSet}

import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraftforge.fml.common.ObfuscationReflectionHelper

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet

import mrtjp.Implicits.IWorldEventListenerExt

object Utils {
  def rescheduleTicks(world: World, blocks: Set[BlockPos], allBlocks: Set[BlockPos], dir: EnumFacing): Unit = world match {
    case world: WorldServer =>
      val hash = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_73064_N",
        "pendingTickListEntriesHashSet").asInstanceOf[JSet[NextTickListEntry]]

      val tree = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_73065_O",
        "pendingTickListEntriesTreeSet").asInstanceOf[JTreeSet[NextTickListEntry]]

      val list = ObfuscationReflectionHelper.getPrivateValue(classOf[WorldServer], world, "field_94579_S",
        "pendingTickListEntriesThisTick").asInstanceOf[JAList[NextTickListEntry]]

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
          // FIXME: AccessTransformer?
          tick.position = bc
        }
      }

      for (tick <- scheduledTicks) if (!hash.contains(tick)) {
        hash.add(tick)
        tree.add(tick)
      }
    case _ =>
  }

  def rerenderBlocks(world: World, blocks: Set[BlockPos]) {
    if (world.isRemote) {
      val mc = Minecraft.getMinecraft
      // FIXME: AccessTransformer?
      val teList = mc.renderGlobal.setTileEntities.asInstanceOf[JList[TileEntity]]
      for (pass <- 0 to 1) {
        for (c <- blocks; te = world.getTileEntity(c)) {
          if (te != null) pass match {
            case 0 => teList.remove(te)
            case 1 => teList.add(te)
          }
          mc.renderGlobal.markBlockRangeForRenderUpdate(c, c)
        }
        // FIXME: wat?
        mc.renderGlobal.updateRenderers(mc.getRenderViewEntity, false)
      }
    }
  }
}