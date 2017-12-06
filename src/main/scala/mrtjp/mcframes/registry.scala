/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes

import mrtjp.Implicits._
import mrtjp.mcframes.api.{IFrame, IFrameInteraction}
import net.minecraft.block.Block
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.util.matching.Regex

object StickRegistry {
  val rKeyVal: Regex = raw"([\w:]+)\s*->\s*(.+)".r
  val rName: Regex = raw"(.+)".r
  val rNameMetaM: Regex = raw"(.+)m(\d+)".r
  val rMod: Regex = raw"mod:(\w+)".r

  var latchMap: Map[(Block, Int), Set[(Block, Int)]] = Map().withDefaultValue(Set())
  var interactionList: Seq[IFrameInteraction] = Seq()

  def parseKV(kv: Seq[String]): Seq[(String, String)] = kv.map {
    case rKeyVal(k, v) => (k, v)
    case s => throw new MatchError(s"Illegal [k -> v] pair: $s")
  }

  def parseBlockMeta(b: String): (Block, Int) = b match {
    case rNameMetaM(name, meta) => Block.getBlockFromName(fixName(name)) -> meta.toInt
    case rName(name) => Block.getBlockFromName(fixName(name)) -> -1
    case _ => throw new MatchError(s"Illegal set part: $b")
  }

  def fixName(name: String): String = name.indexOf(':') match {
    case -1 => "minecraft:" + name
    case _ => name
  }

  def parseAndAddLatchSets(kv: Seq[String]): Array[String] = {
    parseKV(kv).foreach(b => addLatchSet(parseBlockMeta(b._1), parseBlockMeta(b._2)))
    latchMap.flatMap { kv =>
      val (b, i) = kv._1
      val e1 = Block.REGISTRY.getNameForObject(b) + (if (i != -1) s"m$i" else "")
      kv._2.map { k =>
        val (b2, i2) = k
        val e2 = Block.REGISTRY.getNameForObject(b2) + (if (i2 != -1) s"m$i2" else "")
        e1 + " -> " + e2
      }
    }.toArray
  }

  def addLatchSet(b1: (Block, Int), b2: (Block, Int)) {
    latchMap += b1 -> (latchMap(b1) + b2)
  }

  def resolveStick(w: World, pos: BlockPos, side: EnumFacing): Boolean = {
    def getFrame(pos: BlockPos): IFrame = {
      val b = w.getBlockState(pos).getBlock
      if (b.isInstanceOf[IFrame]) return b.asInstanceOf[IFrame]
      val te = w.getTileEntity(pos, classOf[IFrame])
      if (te.isDefined) return te.get
      interactionList.find(_.canInteract(w, pos.getX, pos.getY, pos.getZ)).orNull
    }

    val f1 = getFrame(pos)
    if (f1 != null && f1.stickOut(w, pos, side)) {
      val p2 = pos.offset(side)
      val f2 = getFrame(p2)
      return f2 == null || f2.stickIn(w, p2, side.getOpposite)
    }

    latchSet(w, pos, side)
  }

  def latchSet(w: World, posIn: BlockPos, side: EnumFacing): Boolean = {
    val pos = posIn.offset(side)
    val b1 = w.getBlockState(posIn).blockAndMeta
    val b2 = w.getBlockState(pos).blockAndMeta

    val set = latchMap.getOrElse(b1, latchMap((b1._1, -1)))
    set.contains(b2) || set.contains((b2._1, -1))
  }
}