/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

import java.util.Set;

/**
 * Helper class used to queue and execute the movement of a set of blocks.
 * See {@link mrtjp.mcframes.TileMotor} for usage.
 */
public abstract class Relocator
{
    /**
     * Pushes the instance of the Relocator onto the stack.
     * Must be called before using any methods.
     *
     * @throws IllegalStateException If the Relocator is already on the stack.
     */
    public abstract void push();

    /**
     * Pops the instance of the Relocator off the stack.
     * Must be called after you are done using this.
     *
     * @throws IllegalStateException If the Relocator is not on the stack.
     */
    public abstract void pop();

    /**
     * Sets the world for the movement.
     *
     * @param world The world in which the movement will occure.
     */
    public abstract void setWorld(World world);

    /**
     * Sets the direction towards which all the queued blocks will move.
     * This is a ForgeDirection index, from 0 to 5
     *
     * @param dir The direction to move the blocks.
     *
     * @throws IllegalStateException If the Relocator is not on the stack.
     */
    public abstract void setDirection(int dir);

    /**
     * Following methods are used to add blocks to the queue.
     *
     * @throws IllegalStateException If the Relocator is not on the stack.
     */
    public abstract void addBlock(int x, int y, int z);
    public abstract void addBlock(BlockPos bc);
    public abstract void addBlocks(Set<BlockPos> blocks);

    /**
     * Used to execute the queued movement. Before calling, the world, direction,
     * and queue of blocks must be set.
     *
     * @return True if the movement was successfully started.
     *
     * @throws IllegalStateException If the Relocator is not on the stack.
     */
    public abstract boolean execute();
}