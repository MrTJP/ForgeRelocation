/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.vec.{BlockCoord, Vector3}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.math.MathLib
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.{OpenGlHelper, RenderBlocks, RenderHelper, Tessellator}
import net.minecraft.world.{EnumSkyBlock, IBlockAccess, World}
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11._

object MovingRenderer
{
    var partialTickTime = 0.0f
    var isRendering = false
    var renderHack = true

    def mc = Minecraft.getMinecraft
    def world = mc.theWorld
    def tes = Tessellator.instance

    var oldWorld:World = null
    var renderBlocks:RenderBlocks = null

    def render(x:Int, y:Int, z:Int, d:RenderPos, tick:Float)
    {
        val oldOcclusion = mc.gameSettings.ambientOcclusion
        mc.gameSettings.ambientOcclusion = 0

        val block = world.getBlock(x, y, z)
        if (block == null) return

        val engine = TileEntityRendererDispatcher.instance.field_147553_e
        if (engine != null) engine.bindTexture(TextureMap.locationBlocksTexture)
        mc.entityRenderer.enableLightmap(partialTickTime)

        val light = world.getLightBrightnessForSkyBlocks(x, y, z, block.getLightValue(world, x, y, z))
        val l1 = light%65536
        val l2 = light/65536

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1, l2)
        glColor4f(0, 0, 0, 0)
        RenderHelper.disableStandardItemLighting()
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glDisable(GL_CULL_FACE)
        glShadeModel(if (Minecraft.isAmbientOcclusionEnabled) GL_SMOOTH else GL_FLAT)

        val vec = d.vec.copy+new Vector3(new BlockCoord().offset(d.moveDir))*tick/16F
        for (pass <- 0 to 1)
        {
            tes.startDrawingQuads()
            tes.setTranslation(
                -TileEntityRendererDispatcher.staticPlayerX+MathLib.clamp(-1F, 1F, vec.x.toFloat),
                -TileEntityRendererDispatcher.staticPlayerY+MathLib.clamp(-1F, 1F, vec.y.toFloat),
                -TileEntityRendererDispatcher.staticPlayerZ+MathLib.clamp(-1F, 1F, vec.z.toFloat))
            tes.setColorOpaque(1, 1, 1)

            block.getRenderBlockPass
            if (block.canRenderInPass(pass))
            {
                renderHack = false
                renderBlocks.renderBlockByRenderType(block, x, y, z)
                renderHack = true
            }

            tes.setTranslation(0, 0, 0)
            tes.draw()
        }
        RenderHelper.enableStandardItemLighting()
        mc.entityRenderer.disableLightmap(partialTickTime)
        mc.gameSettings.ambientOcclusion = oldOcclusion
    }

    @SubscribeEvent
    def onRenderWorld(e:RenderWorldLastEvent)
    {
        if (oldWorld != world)
        {
            oldWorld = world
            renderBlocks = new MovingRenderBlocks(new MovingWorld(world))
            renderBlocks.renderAllFaces = true
            MovementManager.clientMap.clear()
        }
        MovementManager.clientMap.synchronized
        {
            val it = MovementManager.clientMap.iterator
            while (it.hasNext)
            {
                it.advance()
                val k = it.key()
                val v = it.value()
                import mrtjp.core.world.WorldLib.{unpackX, unpackY, unpackZ}
                render(unpackX(k), unpackY(k), unpackZ(k), v, partialTickTime)
            }
        }
    }

    def onPreRenderTick(time:Float)
    {
        isRendering = true
        partialTickTime = time
    }

    def onPostRenderTick()
    {
        isRendering = false
    }
}

object RenderTicker
{
    @SubscribeEvent
    def onRenderTick(e:TickEvent.RenderTickEvent)
    {
        if (e.phase == TickEvent.Phase.START)
            MovingRenderer.onPreRenderTick(e.renderTickTime)
        else MovingRenderer.onPostRenderTick()
    }
}

class RenderPos(val vec:Vector3, var moveDir:Int)
{
    def this(x:Float, y:Float, z:Float, moveDir:Int) = this(new Vector3(x, y, z), moveDir)

    def x = vec.x.toFloat
    def y = vec.y.toFloat
    def z = vec.z.toFloat
}

@SideOnly(Side.CLIENT)
class MovingRenderBlocks(w:IBlockAccess) extends RenderBlocks(w)
{
    override def renderStandardBlock(block:Block, x:Int, y:Int, z:Int) =
    {
        import mrtjp.relocation.MovementManager.eps
        if (MovementManager.isMoving(Minecraft.getMinecraft.theWorld, x, y, z))
        {
            if (renderMinX == 0.0D) renderMinX += eps
            if (renderMinY == 0.0D) renderMinY += eps
            if (renderMinZ == 0.0D) renderMinZ += eps
            if (renderMaxX == 1.0D) renderMaxX -= eps
            if (renderMaxY == 1.0D) renderMaxY -= eps
            if (renderMaxZ == 1.0D) renderMaxZ -= eps
        }
        super.renderStandardBlock(block, x, y, z)
    }
}

class MovingWorld(val world:World) extends IBlockAccess
{
    override def getBlock(x:Int, y:Int, z:Int) = world.getBlock(x, y, z)

    override def getTileEntity(x:Int, y:Int, z:Int) = world.getTileEntity(x, y, z)

    def computeLightValue(x:Int, y:Int, z:Int, tpe:EnumSkyBlock) =
    {
        (for (s <- 0 until 6; c = new BlockCoord(x, y, z).offset(s))
        yield world.getSavedLightValue(tpe, c.x, c.y, c.z)).max
    }

    @SideOnly(Side.CLIENT)
    override def getLightBrightnessForSkyBlocks(x:Int, y:Int, z:Int, light:Int) =
        MovementManager.isMoving(Minecraft.getMinecraft.theWorld, x, y, z) match
        {
            case true =>
                val l1 = computeLightValue(x, y, z, EnumSkyBlock.Sky)
                val l2 = computeLightValue(x, y, z, EnumSkyBlock.Block)
                l1<<20|Seq(l2, light).max<<4
            case false => world.getLightBrightnessForSkyBlocks(x, y, z, light)
        }

    override def getBlockMetadata(x:Int, y:Int, z:Int) = world.getBlockMetadata(x, y, z)

    @SideOnly(Side.CLIENT)
    override def isAirBlock(x:Int, y:Int, z:Int) = world.isAirBlock(x, y, z)

    @SideOnly(Side.CLIENT)
    override def getBiomeGenForCoords(x:Int, z:Int) = world.getBiomeGenForCoords(x, z)

    @SideOnly(Side.CLIENT)
    override def getHeight = world.getHeight

    @SideOnly(Side.CLIENT)
    override def extendedLevelsInChunkCache() = world.extendedLevelsInChunkCache()

    override def isBlockProvidingPowerTo(x:Int, y:Int, z:Int, side:Int) =
        world.isBlockProvidingPowerTo(x, y, z, side)

    override def isSideSolid(x:Int, y:Int, z:Int, side:ForgeDirection, _default:Boolean) =
        world.isSideSolid(x, y, z, side, _default)
}