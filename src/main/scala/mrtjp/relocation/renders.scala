/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.reflect.{ObfMapping, ReflectionManager}
import codechicken.lib.vec.Vector3
import mrtjp.core.math.MathLib
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.Minecraft.{getMinecraft => mc}
import net.minecraft.client.renderer.Tessellator.{getInstance => tes}
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.color.BlockColors
import net.minecraft.client.renderer.texture.{TextureAtlasSprite, TextureMap}
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.IResourceManager
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{BlockRenderLayer, EnumBlockRenderType, EnumFacing}
import net.minecraft.world.biome.Biome
import net.minecraft.world.{EnumSkyBlock, IBlockAccess, World, WorldType}
import net.minecraftforge.client.{ForgeHooksClient, MinecraftForgeClient}
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._

object MovingRenderer
{
    var isRendering = false
    var disableUnmovedBlockRender = true

    private var initialized = false

    private var oldWorld:World = _
    private var movingWorld:MovingWorld = _

    private var frame = 0.0f

    def init()
    {
        if (!initialized) {
            val parentDispatcher = mc.getBlockRendererDispatcher
            val newDispatcher = new MovingBlockRenderDispatcher(parentDispatcher, mc.getBlockColors)

            val mapping = new ObfMapping("net/minecraft/client/Minecraft", "field_175618_aM")
            ReflectionManager.setField(mapping, mc, newDispatcher)

            initialized = true
        }
    }

    private def render(pos:BlockPos, rpos:Vector3)
    {
        val block = mc.world.getBlockState(pos)
        if (block.getBlock.isAir(block, mc.world, pos)) return
        if (block.getRenderType == EnumBlockRenderType.INVISIBLE) return

        val oldOcclusion = mc.gameSettings.ambientOcclusion
        mc.gameSettings.ambientOcclusion = 0

        val engine = TileEntityRendererDispatcher.instance.renderEngine
        if (engine != null) engine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        mc.entityRenderer.enableLightmap()

        val light = movingWorld.getCombinedLight(pos, 0)
        val l1 = light % 65536
        val l2 = light / 65536

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1, l2)
        GlStateManager.color(1, 1, 1, 1)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableBlend()
        GlStateManager.shadeModel(if (Minecraft.isAmbientOcclusionEnabled) GL_SMOOTH else GL_FLAT)

        val prevRenderLayer = MinecraftForgeClient.getRenderLayer

        for (layer <- BlockRenderLayer.values()) {
            if (block.getBlock.canRenderInLayer(block, layer)) {

                ForgeHooksClient.setRenderLayer(layer)

                tes.getBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)
                tes.getBuffer.setTranslation(
                    -TileEntityRendererDispatcher.staticPlayerX + MathLib.clamp(-1F, 1F, rpos.x.toFloat),
                    -TileEntityRendererDispatcher.staticPlayerY + MathLib.clamp(-1F, 1F, rpos.y.toFloat),
                    -TileEntityRendererDispatcher.staticPlayerZ + MathLib.clamp(-1F, 1F, rpos.z.toFloat))

                GlStateManager.color(1f, 1f, 1f, 1f)

                disableUnmovedBlockRender = false
                mc.getBlockRendererDispatcher.renderBlock(block, pos, movingWorld, tes.getBuffer)
                disableUnmovedBlockRender = true

                tes.getBuffer.setTranslation(0, 0, 0)
                tes.draw()
            }
        }

        ForgeHooksClient.setRenderLayer(prevRenderLayer)

        RenderHelper.enableStandardItemLighting()
        mc.entityRenderer.disableLightmap()
        mc.gameSettings.ambientOcclusion = oldOcclusion
    }

    def onPreRenderTick(time:Float)
    {
        isRendering = true
        frame = time
    }

    def onRenderWorldEvent()
    {
        if (oldWorld != mc.world) {
            oldWorld = mc.world
            movingWorld = new MovingWorld(mc.world)
            // renderBlocks.renderAllFaces = true
            // TODO make block renderer not check sides
        }

        for (s <- MovementManager2.getWorldStructs(mc.world).structs) for (b <- s.preMoveBlocks)
            render(b, renderPos(s, frame))
    }

    def onPostRenderTick()
    {
        isRendering = false
    }

    def renderPos(s:BlockStruct, partial:Float) =
        new Vector3(s.moveDir.getDirectionVec.getX, s.moveDir.getDirectionVec.getY, s.moveDir.getDirectionVec.getZ)
                .multiply(s.progress + s.speed * partial)
}

class MovingWorld(val parentWorld:World) extends IBlockAccess
{
    def computeLightValue(pos:BlockPos, tpe:EnumSkyBlock) =
    {
        (for (s <- EnumFacing.VALUES; c = pos.offset(s))
            yield parentWorld.getLightFor(tpe, c)).max
    }

    override def getCombinedLight(pos:BlockPos, lightValue:Int):Int =
        if (MovementManager2.isMoving(Minecraft.getMinecraft.world, pos)) {
            val l1 = computeLightValue(pos, EnumSkyBlock.SKY)
            val l2 = computeLightValue(pos, EnumSkyBlock.BLOCK)
            l1 << 20 | Seq(l2, lightValue).max << 4
        } else {
            parentWorld.getCombinedLight(pos, lightValue)
        }

    override def getTileEntity(pos:BlockPos):TileEntity = parentWorld.getTileEntity(pos)
    override def getBlockState(pos:BlockPos):IBlockState = parentWorld.getBlockState(pos)
    override def isAirBlock(pos:BlockPos):Boolean = parentWorld.isAirBlock(pos)
    override def getBiome(pos:BlockPos):Biome = parentWorld.getBiome(pos)
    override def getStrongPower(pos:BlockPos, direction:EnumFacing):Int = parentWorld.getStrongPower(pos, direction)
    override def getWorldType:WorldType = parentWorld.getWorldType
    override def isSideSolid(pos:BlockPos, side:EnumFacing, _default:Boolean):Boolean = parentWorld.isSideSolid(pos, side, _default)
}

class MovingBlockRenderDispatcher(val parentDispatcher:BlockRendererDispatcher, colors:BlockColors) extends BlockRendererDispatcher(parentDispatcher.getBlockModelShapes, colors)
{
    override def renderBlock(state:IBlockState, pos:BlockPos, blockAccess:IBlockAccess, bufferBuilderIn:BufferBuilder) =
    {
        if (MovingRenderer.disableUnmovedBlockRender && MovementManager2.isMoving(Minecraft.getMinecraft.world, pos))
            false
        else
            parentDispatcher.renderBlock(state, pos, blockAccess, bufferBuilderIn)
    }

    override def renderBlockDamage(state:IBlockState, pos:BlockPos, texture:TextureAtlasSprite, blockAccess:IBlockAccess) {
        parentDispatcher.renderBlockDamage(state, pos, texture, blockAccess)
    }

    override def renderBlockBrightness(state:IBlockState, brightness:Float) {
        parentDispatcher.renderBlockBrightness(state, brightness)
    }

    override def onResourceManagerReload(resourceManager:IResourceManager) {
        parentDispatcher.onResourceManagerReload(resourceManager)
    }

    override def getBlockModelRenderer = parentDispatcher.getBlockModelRenderer
    override def getModelForState(state:IBlockState) = parentDispatcher.getModelForState(state)
    override def getBlockModelShapes = parentDispatcher.getBlockModelShapes
}