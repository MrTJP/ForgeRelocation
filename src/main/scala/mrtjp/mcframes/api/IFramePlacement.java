/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.mcframes.api;

import codechicken.lib.vec.Vector3;
import mrtjp.relocation.api.RelocationAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A utility interface used by the frame ItemBlock when placing.
 * It can be used to add interactions to the default frame block
 * placement. This class can be registered through the
 * {@link RelocationAPI}.
 */
public interface IFramePlacement {
    /**
     * Called sequentially on all registered IFrameBlockPlacements on
     * the frame item block's onItemUse function, until one returns true.
     *
     * @param item   The itemstack of the frame block.
     * @param player The player who is placing the block.
     * @param world  The world the block is being placed in.
     * @param x      The x coordinate that the block is being placed in.
     * @param y      The y coordinate that the block is being placed in.
     * @param z      The z coordinate that the block is being placed in.
     * @param side   The side the player clicked on
     * @param hit    The exact hit position of the click.
     * @return True if something happened. This will subsequently block the actual placement of the block.
     */
    public boolean onItemUse(ItemStack item, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, Vector3 hit);
}