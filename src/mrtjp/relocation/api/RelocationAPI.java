/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
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
                  <modID>:<blockname>m<meta> - to assign block from mod for specific meta
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
                  <modID>:<blockname>m<meta> - to assign block from mod for specific meta
     *
     * @param value The name of the mover to assign this block. A list of all available
     *              movers will show up in the configs.
     */
    public abstract void registerMandatoryMover(String key, String value);

    /**
     * Used to register a custom placment that can define how a frame is placed.
     * This is useful for cases such as if the frame is placed on a Multipart from
     * the Forge Multipart API, this placement property can perform special actions
     * that would add the frame to the multipart tile.
     *
     * @param placement The custom frame placement to register.
     */
    public abstract void registerFramePlacement(IFramePlacement placement);

    /**
     * Used to register a {@link IFrameInteraction}, which is a class that
     * can be used to add frame-like properties to any block.
     *
     * @param interaction The interaction to register.
     */
    public abstract void registerFrameInteraction(IFrameInteraction interaction);

    /**
     * Getter for the instance variable of the Frame block. Used if external
     * implementations need the block. (i.e. registration purposes)
     *
     * @return The frame block.
     */
    public abstract Block getFrameBlock();

    /**
     * Getter for the instance variable of the Motor block. Used if external
     * implementations need the block. (i.e. registration purposes)
     *
     * @return The motor block.
     */
    public abstract Block getMotorBlock();


    /** Utilities - used for silent block manipulation. Caution: May be removed.. **/

    /**
     * Used to set a block in the world to the given block without
     * alerting the world of the change.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param b The block.
     * @param meta Block metadata
     */
    public abstract void uncheckedSetBlock(World w, int x, int y, int z, Block b, int meta);

    /**
     * Used to set a tile entity in the world without alerting the
     * world of the change.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param te The TileEntity.
     */
    public abstract void uncheckedSetTileEntity(World w, int x, int y, int z, TileEntity te);

    /**
     * Used to remove a tile entity in the world without alerting the
     * world of the change.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     */
    public abstract void uncheckedRemoveTileEntity(World w, int x, int y, int z);

    /**
     * Used to retreive a tile from a world without alerting the world
     * that the tile was fetched from memory.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return The TileEnity at the given coordinates
     */
    public abstract TileEntity uncheckedGetTileEntity(World w, int x, int y, int z);


    /**
     * Tessellates the frame model at the given coordinates. Useful for making
     * your block look like the default frame block.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     */
    public abstract void renderFrame(double x, double y, double z);

    /**
     * Raytrace a frame block at the given world coords with start and end
     * points of a ray.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param start The start point of the ray
     * @param end The end point of the ray
     * @return The object position if anything from the frame model was hit.
     */
    public abstract MovingObjectPosition raytraceFrame(double x, double y, double z, Vec3 start, Vec3 end);
}
