/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.api;

import mrtjp.relocation.api.Relocator;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Interface that can be implemented on Tile Entities or Blocks that wish to
 * act as frames, which are the blocks that stick together and form a
 * moving structure when moved through the {@link Relocator}. No other action besides
 * implementation of this interface is needed for the block to function.
 */
public interface IFrame {
    /**
     * Used to check if this frame block is allowed to grab
     * a block on the given side.
     * <p>
     * Convention requires that this method yields the same result
     * on both client and server.
     *
     * @param w    The world.
     * @param pos  The block's position.
     * @param side The side of this block, as a ForgeDirection
     * @return True if the side can grab another block.
     */
    boolean stickOut(World w, BlockPos pos, EnumFacing side);

    /**
     * Used to check if this frame block can be grabbed on the
     * given side by another frame.
     * <p>
     * Convention requires that this method yields the same result
     * on both client and server.
     *
     * @param w    The world.
     * @param pos  The block's position.
     * @param side The side of this block, as a ForgeDirection
     * @return True if the side can be grabbed by a frame block.
     */
    boolean stickIn(World w, BlockPos pos, EnumFacing side);
}