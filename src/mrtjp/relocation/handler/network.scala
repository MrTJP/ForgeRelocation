/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.handler

import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.util.{LinkedList => JLinkedList, List => JList}

import codechicken.lib.data.MCDataOutputWrapper
import codechicken.lib.packet.PacketCustom
import codechicken.lib.packet.PacketCustom.{IClientPacketHandler, IServerPacketHandler}
import mrtjp.relocation.MovementManager2
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.network.play.{INetHandlerPlayClient, INetHandlerPlayServer}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
import net.minecraft.world.{ChunkCoordIntPair, World}

import scala.collection.JavaConversions._
import scala.collection.mutable.{HashMap => MHashMap, Map => MMap, MultiMap => MMultiMap, Set => MSet}

/**
 * Tweaked version of Chickenbones' compressed end-of-tick tile data stream.
 */
class RelocationPH
{
    val channel = RelocationMod
}

object RelocationCPH extends RelocationPH with IClientPacketHandler
{
    def handlePacket(packet: PacketCustom, mc: Minecraft, netHandler:INetHandlerPlayClient) {
        try {
            packet.getType match {
                case 1 => handleChunkDesc(packet, mc.theWorld)
                case 2 => handleChunkData(packet, mc.theWorld)
            }
        }
        catch {
            case e:RuntimeException if e.getMessage.startsWith("DC: ") =>
                netHandler.handleDisconnect(new S40PacketDisconnect(new ChatComponentText(e.getMessage.substring(4))))
        }
    }

    def handleChunkData(packet:PacketCustom, world:World)
    {
        var i = packet.readUByte()
        while (i != 255) {
            MovementManager2.read(world, packet, i)
            i = packet.readUByte()
        }
    }

    def handleChunkDesc(packet:PacketCustom, world:World)
    {
        MovementManager2.readDesc(world, packet)
    }
}

object RelocationSPH extends RelocationPH with IServerPacketHandler
{
    class MCByteStream(bout:ByteArrayOutputStream) extends MCDataOutputWrapper(new DataOutputStream(bout))
    {
        def getBytes = bout.toByteArray
    }

    override def handlePacket(packetCustom:PacketCustom, entityPlayerMP:EntityPlayerMP, iNetHandlerPlayServer:INetHandlerPlayServer){}

    private val updateMap = MMap[World, MMap[Set[ChunkCoordIntPair], MCByteStream]]()
    private val chunkWatchers = new MHashMap[Int, MSet[ChunkCoordIntPair]] with MMultiMap[Int, ChunkCoordIntPair]
    private val newWatchers = MMap[Int, JLinkedList[ChunkCoordIntPair]]()

    def onTickEnd()
    {
        val players = getServerPlayers
        sendData(players)
        sendDesc(players)
    }

    def onWorldUnload(world:World)
    {
        if(!world.isRemote)
        {
            updateMap.remove(world)
            if (chunkWatchers.nonEmpty)
            {
                val players = getServerPlayers
                for (p <- players) if (p.worldObj.provider.dimensionId == world.provider.dimensionId)
                    chunkWatchers.remove(p.getEntityId)
            }
        }
    }

    def onChunkWatch(p:EntityPlayer, c:ChunkCoordIntPair)
    {
        newWatchers.getOrElseUpdate(p.getEntityId, new JLinkedList).add(c)
    }

    def onChunkUnWatch(p:EntityPlayer, c:ChunkCoordIntPair)
    {
        newWatchers.get(p.getEntityId) match
        {
            case Some(chunks) => chunks.remove(c)
            case _ =>
        }
        chunkWatchers.removeBinding(p.getEntityId, c)
    }

    private def getServerPlayers:Seq[EntityPlayerMP] =
        MinecraftServer.getServer.getConfigurationManager.playerEntityList
                .asInstanceOf[JList[EntityPlayerMP]]

    private def sendData(players:Seq[EntityPlayerMP])
    {
        for(p <- players if chunkWatchers.containsKey(p.getEntityId))
        {
            updateMap.get(p.worldObj) match
            {
                case Some(m) if m.nonEmpty =>
                    val chunks = chunkWatchers(p.getEntityId)
                    val packet = new PacketCustom(channel, 2).compress()
                    var send = false
                    for((uchunks, stream) <- m if uchunks.exists(chunks.contains))
                    {
                        send = true
                        packet.writeByteArray(stream.getBytes)
                        packet.writeByte(255) //terminator
                    }
                    if(send) packet.sendToPlayer(p)
                case _ =>
            }
        }
        updateMap.foreach(_._2.clear())
    }

    private def sendDesc(players:Seq[EntityPlayerMP])
    {
        for(p <- players if newWatchers.containsKey(p.getEntityId))
        {
            val watched = newWatchers(p.getEntityId)
            val pkt = getDescPacket(p.worldObj, watched.toSet)
            if(pkt != null) pkt.sendToPlayer(p)
            for(c <- watched)
                chunkWatchers.addBinding(p.getEntityId, c)
        }
        newWatchers.clear()
    }

    private def getDescPacket(world:World, chunks:Set[ChunkCoordIntPair]):PacketCustom =
    {
        val packet = new PacketCustom(channel, 1)
        if(MovementManager2.writeDesc(world, chunks, packet)) packet else null
    }

    def forceSendData()
    {
        sendData(getServerPlayers)
    }

    def forceSendDesc()
    {
        sendDesc(getServerPlayers)
    }

    def getStream(world:World, chunks:Set[ChunkCoordIntPair], key:Int) =
    {
        updateMap.getOrElseUpdate(world,
        {
            if(world.isRemote)
                throw new IllegalArgumentException("Cannot use RelocationSPH on a client world")
            MMap()
        }).getOrElseUpdate(chunks,
        {
            val s = new MCByteStream(new ByteArrayOutputStream)
            s.writeByte(key)
            s
        })
    }
}