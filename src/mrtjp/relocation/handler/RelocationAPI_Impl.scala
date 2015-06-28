/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import mrtjp.relocation.api.{ITileMover, RelocationAPI}
import mrtjp.relocation.{MovementManager2, MovingTileRegistry}
import net.minecraft.world.World

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