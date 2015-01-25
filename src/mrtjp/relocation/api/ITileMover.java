/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

/**
 * Interface which when implemented manages the movement of blocks and tiles that are
 * registered to it. This class should be registered through the {@link RelocationAPI}.
 */
public interface ITileMover
{
    /**
     * Used to check if the block at the given position can move. This
     * method is only called if the specified block is tied to this
     * handler, so there is no need to check if the block is valid
     * for this handler. This is called before actually moving. If
     * everything is able to move, an animation will start.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return True if the block at the given position is able to move.
     */
    public boolean canMove(World w, int x, int y, int z);

    /**
     * Method used to actually move the tile. Called after the animation
     * has run.  This is where you should tell the tile that it is time
     * to move, and peform any extra checks or calls. This is called
     * on every block in the moved structure sequentially.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param side The ForgeDirection side the structure is moving in.
     */
    public void move(World w, int x, int y, int z, int side);

    /**
     * Called after all blocks in the group have moved to their
     * new locations. This is where you would reload your tile,
     * tell it to refresh or reacknowledge its new position.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     */
    public void postMove(World w, int x, int y, int z);
}