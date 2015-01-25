/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import mrtjp.core.block.InstancedBlockTile
import net.minecraft.nbt.NBTTagCompound

trait TPowerTileCommon extends InstancedBlockTile
{
    def getEnergy:Double
    def setEnergy(en:Double)

    def maxEnergy:Double

    abstract override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        setEnergy(tag.getDouble("energy"))
    }

    abstract override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        tag.setDouble("energy", getEnergy)
    }
}

trait TInfinitePower extends TPowerTileCommon
{
    override def getEnergy = Double.MaxValue
    override def setEnergy(en:Double){}

    override def maxEnergy = Double.MaxValue
}

trait TPowerTile extends TPowerTileCommon with TInfinitePower//TODO mixin with all implementors of TPowerTile