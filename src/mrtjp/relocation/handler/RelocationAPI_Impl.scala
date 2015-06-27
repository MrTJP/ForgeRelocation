/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import java.util.{Set => JSet}

import codechicken.lib.vec.BlockCoord
import mrtjp.relocation.api.{BlockPos, ITileMover, RelocationAPI, Relocator}
import mrtjp.relocation.{MovementManager2, MovingTileRegistry}
import net.minecraft.world.World

import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}

object RelocationAPI_Impl extends RelocationAPI
{
    var isPreInit = true

    override def registerTileMover(name:String, desc:String, handler:ITileMover) =
    {
        assert(isPreInit)
        MovingTileRegistry.registerTileMover(name, desc, handler)
    }

    override def registerPreferredMover(key:String, value:String)
    {
        assert(isPreInit)
        MovingTileRegistry.preferredMovers :+= (key, value)
    }

    override def registerMandatoryMover(key:String, value:String)
    {
        assert(isPreInit)
        MovingTileRegistry.mandatoryMovers :+= (key, value)
    }

    override def getRelocator = Relocator_Impl

    override def isMoving(world:World, x:Int, y:Int, z:Int) =
        MovementManager2.isMoving(world, x, y, z)
}

object Relocator_Impl extends Relocator
{
    private var enabled = false

    private var world:World = null
    private var dir = -1
    private val blocks = MSet[BlockCoord]()

    private def assertState()
    {
        if (!enabled) throw new IllegalStateException("Relocator must be on the stack before use.")
    }

    override def push()
    {
        if (enabled) throw new IllegalStateException("Relocator is already on the stack.")
        enabled = true
    }

    override def pop()
    {
        if (!enabled) throw new IllegalStateException("Relocator is not on the stack.")
        enabled = false
        world = null
        dir = -1
        blocks.clear()
    }

    override def setWorld(w:World)
    {
        assertState()
        if (world != null) throw new IllegalStateException("World already set.")
        world = w
    }

    override def setDirection(d:Int)
    {
        assertState()
        if (dir != -1) throw new IllegalStateException("Direction already set.")
        dir = d
    }

    override def addBlock(x:Int, y:Int, z:Int)
    {
        assertState()
        blocks += new BlockCoord(x, y, z)
    }

    override def addBlock(bc:BlockPos)
    {
        addBlock(bc.x, bc.y, bc.z)
    }

    override def addBlocks(blocks:JSet[BlockPos])
    {
        for (b <- blocks) addBlock(b)
    }

    override def execute() =
    {
        assertState()
        if (world == null)  throw new IllegalStateException("World must be set before move.")
        if (world.isRemote) throw new IllegalStateException("Movements cannot be executed client-side.")
        if (dir == -1)      throw new IllegalStateException("Direction must be set before move.")
        if (blocks.isEmpty) throw new IllegalStateException("At least 1 block is required for move.")
        MovementManager2.tryStartMove(world, blocks.toSet, dir)
    }
}