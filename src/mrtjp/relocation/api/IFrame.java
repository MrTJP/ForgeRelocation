/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

/**
 * Interface that can be implemented on Tile Entities or Blocks that wish to
 * act as frames, which are the blocks that stick together and form a
 * moving structure when moved with a motor. No other action besides
 * implementation of this interface is needed for the block to function.
 */
public interface IFrame
{
    /**
     * Used to check if the given side on this frame block is sticky.
     * Sticky sides will allow other frames to stick to this one,
     * as well as other blocks.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param side The side of the block, as a ForgeDirection
     * @return True if the side can latch on to another block.
     */
    public boolean latchSide(World w, int x, int y, int z, int side);
}