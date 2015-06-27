/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.api;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * API class for MCFrames.
 * <br><br>
 * It is recommended that mods access this class within a soft dependency class.
 */
public abstract class MCFramesAPI
{
    /**
     * This field will contain an implementor of this API if Relocation is installed.
     */
    public static MCFramesAPI instance;

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
     * Getter for the optional StickResolver object which can be used to quicky
     * resolve a structure based on default Relocation frame blocks and stick
     * rules.
     *
     * @return The StickResolver object
     */
    public abstract StickResolver getStickResolver();

    /**
     * Getter for the instance variable of the Frame block. Used if external
     * implementations need the block. (i.e. registration purposes)
     *
     * @return The frame block.
     */
    public abstract Block getFrameBlock();

    /**
     * Tessellates the frame model at the given coordinates. Useful for making
     * your block look like the default frame block.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param mask The mask of sides not to render.
     */
    public abstract void renderFrame(double x, double y, double z, int mask);

    /**
     * Raytrace a frame block at the given world coords with start and end
     * points of a ray.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @param mask The side mask for the model.
     * @param start The start point of the ray.
     * @param end The end point of the ray.
     * @return The object position if anything from the frame model was hit.
     */
    public abstract MovingObjectPosition raytraceFrame(double x, double y, double z, int mask, Vec3 start, Vec3 end);
}