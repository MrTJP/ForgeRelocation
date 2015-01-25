/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import net.minecraft.world.World;

/**
 * Class used instead of {@link IFrame} to add frame capabilities
 * to a block without having the block or tile in that location
 * implement the IFrame interface. This class must be registered in
 * the {@link RelocationAPI}.
 */
public abstract class IFrameInteraction implements IFrame
{
    /**
     * Check to see if this interaction can be executed at the given
     * coordinates.
     *
     * @param w The world.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return True if this interaction is valid for the given
     *         location.
     */
    public abstract boolean canInteract(World w, int x, int y, int z);
}
