/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

/**
 * Lightweight block position object used to keep dependencies confined
 * to the api package, used by some of the API's methods.
 */
public class BlockPos
{
    public int x;
    public int y;
    public int z;

    public BlockPos(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object obj)
    {
        if(!(obj instanceof BlockPos)) return false;
        else
        {
            BlockPos o2 = (BlockPos) obj;
            return this.x == o2.x && this.y == o2.y && this.z == o2.z;
        }
    }

    public int hashCode()
    {
        return (this.x^this.z)*31+this.y;
    }
}