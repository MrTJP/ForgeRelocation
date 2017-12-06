/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import mrtjp.relocation.handler.RelocationSPH
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.{ChunkWatchEvent, WorldEvent}
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object RelocationEventHandler {
  @SubscribeEvent
  def worldUnload(event: WorldEvent.Unload) {
    RelocationSPH.onWorldUnload(event.getWorld)
    MovementManager2.onWorldUnload(event.getWorld)
  }

  @SubscribeEvent
  def chunkWatch(event: ChunkWatchEvent.Watch) {
    RelocationSPH.onChunkWatch(event.getPlayer, event.getChunk)
  }

  @SubscribeEvent
  def chunkUnwatch(event: ChunkWatchEvent.UnWatch) {
    RelocationSPH.onChunkUnWatch(event.getPlayer, event.getChunk)
  }

  @SubscribeEvent
  def serverTick(event: TickEvent.ServerTickEvent) {
    if (event.phase == TickEvent.Phase.END) {
      RelocationSPH.onTickEnd()
      MovementManager2.onTick(false)
    }
  }
}

object RelocationClientEventHandler {
  @SubscribeEvent
  def onRenderTick(e: TickEvent.RenderTickEvent) {
    //    if (e.phase == TickEvent.Phase.START)
    //      MovingRenderer.onPreRenderTick(e.renderTickTime)
    //    else MovingRenderer.onPostRenderTick()
  }

  @SubscribeEvent
  def onRenderWorld(e: RenderWorldLastEvent) {
    //    MovingRenderer.onRenderWorldEvent()
  }

  @SubscribeEvent
  def clientTick(event: TickEvent.ClientTickEvent) {
    if (event.phase == TickEvent.Phase.END)
      MovementManager2.onTick(true)
  }
}