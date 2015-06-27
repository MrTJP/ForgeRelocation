/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

/**
 * Interface for an object that manages the movement of blocks and tiles that are
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
     * Called server-side only when determining what to move.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return True if the block at the given position is able to move.
     */
    boolean canMove(World w, int x, int y, int z);

    /**
     * Method used to actually move the tile. Called after the animation
     * has run.  This is where you should tell the tile that it is time
     * to move, and peform any extra checks or calls. This should also
     * move the block and tile as well.  This is called
     * on every block in the moving structure sequentially.
     *
     * Called on both server and client.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param dir The ForgeDirection the structure is moving in.
     */
    void move(World w, int x, int y, int z, int dir);

    /**
     * Called after all blocks in the group have moved to their
     * new locations. This is where you would reload your tile,
     * tell it to refresh or reacknowledge its new position.
     *
     * Callod on both server and client.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     */
    void postMove(World w, int x, int y, int z);
}