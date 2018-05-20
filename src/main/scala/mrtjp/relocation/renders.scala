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
import net.minecraft.crash.{CrashReport, CrashReportCategory}
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{BlockRenderLayer, EnumBlockRenderType, EnumFacing, ReportedException}
import net.minecraft.world.biome.Biome
import net.minecraft.world.{EnumSkyBlock, IBlockAccess, World, WorldType}
import net.minecraftforge.client.{ForgeHooksClient, MinecraftForgeClient}
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._

object MovingRenderer
{
    var isRendering = false
    var allowQueuedBlockRender = false

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

    private def render(currentPos:BlockPos, moveDir:EnumFacing, startOfRow:BlockPos, endOfRow:BlockPos, rpos:Vector3)
    {
        val block = mc.world.getBlockState(currentPos)
        if (block.getBlock.isAir(block, mc.world, currentPos)) return
        if (block.getRenderType == EnumBlockRenderType.INVISIBLE) return

        movingWorld.locate(currentPos, moveDir, startOfRow, endOfRow)

        val oldOcclusion = mc.gameSettings.ambientOcclusion
//        mc.gameSettings.ambientOcclusion = 0

        val engine = TileEntityRendererDispatcher.instance.renderEngine
        if (engine != null) engine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        mc.entityRenderer.enableLightmap()

        RenderHelper.enableStandardItemLighting()
        val light = movingWorld.getCombinedLight(currentPos, 0)
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

                allowQueuedBlockRender = true
                mc.getBlockRendererDispatcher.renderBlock(block, currentPos, movingWorld, tes.getBuffer)
                allowQueuedBlockRender = false

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

        for (s <- MovementManager2.getWorldStructs(mc.world).structs)
            for (r <- s.rows) for (b <- r.preMoveBlocks)
            render(b, s.moveDir, r.allBlocks.head, r.allBlocks.last, renderPos(s, frame))
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
    var currentPos:BlockPos = _
    var newPos:BlockPos = _
    var moveDir:EnumFacing = _

    var firstBlockInRow:BlockPos = _
    var lastBlockInRow:BlockPos = _

    var disableOffset = false
    var isCalculatingLight = false

    def locate(pos:BlockPos, dir:EnumFacing, first:BlockPos, last:BlockPos)
    {
        currentPos = pos
        newPos = pos.offset(dir)
        moveDir = dir

        firstBlockInRow = first
        lastBlockInRow = last
    }

    def transformPos(pos:BlockPos):BlockPos = if (disableOffset) pos else pos.offset(moveDir)
    
    override def getCombinedLight(pos:BlockPos, lightValue:Int):Int =
    {
        isCalculatingLight = true

        val tpos = transformPos(pos)
        var lightS0 = parentWorld.getLightFromNeighborsFor(EnumSkyBlock.SKY, tpos)
        var lightB0 = math.max(lightValue, parentWorld.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, tpos))

        var lightS1 = parentWorld.getLightFromNeighborsFor(EnumSkyBlock.SKY, pos)
        var lightB1 = math.max(lightValue, parentWorld.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos))

        if (lightS0 == 0 && lightB0 == 0) {
            lightS0 = lightS1
            lightB0 = lightB1
        } else if (lightS1 == 0 && lightB1 == 0) {
            lightS1 = lightS0
            lightB1 = lightB0
        }

        val lightSMid = (lightS0+lightS1)/2
        val lightBMid = (lightB0+lightB1)/2

        val light = lightSMid << 20 | lightBMid << 4

        isCalculatingLight = false

        light
    }

    override def getTileEntity(pos:BlockPos):TileEntity =
    {
        if (!isCalculatingLight)
            return parentWorld.getTileEntity(pos)

        parentWorld.getTileEntity(transformPos(pos))
    }

    override def getBlockState(pos:BlockPos):IBlockState =
    {
        if (!isCalculatingLight)
            return parentWorld.getBlockState(pos)

        val offsetPos = transformPos(pos)

        if (offsetPos == firstBlockInRow)
            return Blocks.AIR.getDefaultState

        parentWorld.getBlockState(offsetPos)
    }

    override def isAirBlock(pos:BlockPos):Boolean = parentWorld.isAirBlock(pos)

    override def getBiome(pos:BlockPos):Biome = parentWorld.getBiome(pos)
    override def getStrongPower(pos:BlockPos, direction:EnumFacing):Int = parentWorld.getStrongPower(pos, direction)
    override def getWorldType:WorldType = parentWorld.getWorldType
    override def isSideSolid(pos:BlockPos, side:EnumFacing, _default:Boolean):Boolean = parentWorld.isSideSolid(pos, side, _default)
}

class MovingBlockRenderDispatcher(val parentDispatcher:BlockRendererDispatcher, colors:BlockColors) extends BlockRendererDispatcher(parentDispatcher.getBlockModelShapes, colors)
{
    override def renderBlock(state:IBlockState, pos:BlockPos, blockAccess:IBlockAccess, bufferBuilderIn:BufferBuilder):Boolean =
    {
        val isMoving = MovementManager2.isMoving(Minecraft.getMinecraft.world, pos)

        if (!MovingRenderer.allowQueuedBlockRender && isMoving) return false

        val isAdjacentMoving = MovementManager2.isAdjacentToMoving(Minecraft.getMinecraft.world, pos)
        if (!isAdjacentMoving)
            return parentDispatcher.renderBlock(state, pos, blockAccess, bufferBuilderIn)

        try {
            val enumblockrendertype = state.getRenderType
            if (enumblockrendertype == EnumBlockRenderType.INVISIBLE) return false

            var state2:IBlockState = state

            if (blockAccess.getWorldType != WorldType.DEBUG_ALL_BLOCK_STATES)
                try
                    state2 = state.getActualState(blockAccess, pos)
                catch {
                    case _:Exception =>
                }

            enumblockrendertype match {
                case EnumBlockRenderType.MODEL if isAdjacentMoving =>
                    val model = this.getModelForState(state)
                    state2 = state.getBlock.getExtendedState(state, blockAccess, pos)
                    getBlockModelRenderer.renderModel(blockAccess, model, state2, pos, bufferBuilderIn, false)
                case _ =>
                    parentDispatcher.renderBlock(state, pos, blockAccess, bufferBuilderIn)
            }
        }
        catch {
            case throwable:Throwable =>
                val crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block in world")
                val crashreportcategory = crashreport.makeCategory("Block being tesselated")
                CrashReportCategory.addBlockInfo(crashreportcategory, pos, state.getBlock, state.getBlock.getMetaFromState(state))
                throw new ReportedException(crashreport)
        }
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