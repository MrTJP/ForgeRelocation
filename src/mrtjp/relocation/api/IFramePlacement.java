/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.api;

import codechicken.lib.vec.Vector3;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A utility interface used by the frame ItemBlock when placing.
 * It can be used to add interactions to the default frame block
 * placement. This class can be registered through the
 * {@link RelocationAPI}.
 */
public interface IFramePlacement
{
    /**
     * Called sequentially on all registered IFrameBlockPlacements on
     * the frame item block's onItemUse function, until one returns true.
     * @param item
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     * @param side
     * @param hit
     * @return
     */
    public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, Vector3 hit);
}