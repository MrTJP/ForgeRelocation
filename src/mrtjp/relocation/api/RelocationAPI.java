/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

/**
 * Central API class for Relocation.
 * If Relocation is installed, the appropriate field will contain an
 * implementor of these methods.
 * <br><br>
 * It is recommended that mods access this class within a soft dependency class.
 */
public abstract class RelocationAPI
{
    /**
     * This field will contain an implementor of this API if Relocation is installed.
     */
    public static RelocationAPI instance;

    /**
     * Used to register a {@link ITileMover} class that manages the movement of certain tiles.
     * This method must be called during FML pre-initialization.
     *
     * @param name The name of the mover that will be used to assign blocks to it.
     * @param desc The description of what this mover is used for or why it is included.
     * @param mover The {@link ITileMover} to register.
     */
    public abstract void registerTileMover(String name, String desc, ITileMover mover);

    /**
     * Used to register an in-game block to a specific {@link ITileMover} which are registered
     * with the method above. This method adds the key-value pairs similar to the config,
     * but their assignments can be changed from the config file if you would like.
     * This method must be called during FML pre-initialization.
     *
     * @param key The mod or block key regex. This works the same way it does from the
     *            config file:
     *            Possible keys:
     *            default - to assign default mover
     *            mod:<modID> - to assign every block from a mod
     *            <modID>:<blockname> - to assign block from a mod for every meta
     *            <modID>:<blockname>m<meta> - to assign block from mod for specific meta
     *
     * @param value The name of the mover to assign this block. A list of all available
     *              movers will show up in the configs.
     */
    public abstract void registerPreferredMover(String key, String value);

    /**
     * Used to register an in-game block to a specific {@link ITileMover} which are registered
     * with the method above. This method adds the key-value pairs similar to the config. These
     * assignments are manatory, and blocks that are registered will be locked to the specified
     * mover, and it cannot be changed from the config.
     * This method must be called during FML pre-initialization.
     *
     * @param key The mod or block key regex. This works the same way it does from the
     *            config file:
     *            Possible keys:
     *            default - to assign default mover
     *            mod:<modID> - to assign every block from a mod
     *            <modID>:<blockname> - to assign block from a mod for every meta
     *            <modID>:<blockname>m<meta> - to assign block from mod for specific meta
     *
     * @param value The name of the mover to assign this block. A list of all available
     *              movers will show up in the configs.
     */
    public abstract void registerMandatoryMover(String key, String value);

    /**
     * Getter for the global Relocator object which is what is used
     * to actually initiate movements.
     *
     * @return The Relocator object
     */
    public abstract Relocator getRelocator();

    /**
     * Used to check if the given block is currently moving. This method is
     * client and server safe.
     *
     * @param world The world the block is in.
     * @param x The x coordinate of the block.
     * @param y The y coordinate of the block.
     * @param z The z coordinate of the block.
     *
     * @return True if the block is currently moving.
     */
    public abstract boolean isMoving(World world, int x, int y, int z);
}