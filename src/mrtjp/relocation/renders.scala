/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation

import codechicken.lib.lighting.LightModel
import codechicken.lib.render.CCModel._
import codechicken.lib.render._
import codechicken.lib.render.uv.{IconTransformation, MultiIconTransformation, UVTransformation}
import codechicken.lib.vec._
import mrtjp.core.block.TInstancedBlockRender
import mrtjp.core.vec.InvertX
import mrtjp.core.world.WorldLib
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess

//TODO use cube map renderer
object MotorRenderer extends TInstancedBlockRender
{
    var bottom:IIcon = _
    var side:IIcon = _
    var sidew:IIcon = _
    var sidee:IIcon = _
    var top:IIcon = _
    var iconT:UVTransformation = _

    val models = Array.ofDim[CCModel](6, 4)

    {
        val base = CCModel.quadModel(24).generateBlock(0, Cuboid6.full)
        for (s <- 0 until 6) for (r <- 0 until 4)
        {
            val m = base.copy.apply(Rotation.sideOrientation(s, r).at(Vector3.center))
            m.computeNormals()
            m.computeLighting(LightModel.standardLightModel)
            models(s)(r) = m
        }
    }

    override def renderWorldBlock(r:RenderBlocks, w:IBlockAccess, x:Int, y:Int, z:Int, meta:Int)
    {
        val te = WorldLib.getTileEntity(w, x, y, z, classOf[TileMotor])
        if (te != null)
        {
            TextureUtils.bindAtlas(0)
            CCRenderState.reset()
            CCRenderState.lightMatrix.locate(w, x, y, z)
            CCRenderState.setBrightness(w, x, y, z)
            models(te.side)(te.rotation).render(new Translation(x, y, z), iconT)
        }
    }

    override def getIcon(s:Int, meta:Int) = s match
    {
        case 0 => bottom
        case 1 => top
        case 2 => side
        case 3 => side
        case 4 => sidew
        case 5 => sidee
    }

    private val invTranslation = new Translation(0, -0.075, 0)
    override def renderInvBlock(r:RenderBlocks, meta:Int)
    {
        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.setDynamic()
        CCRenderState.pullLightmap()
        CCRenderState.setPipeline(iconT, invTranslation)
        CCRenderState.startDrawing()
        BlockRenderer.renderCuboid(Cuboid6.full, 0)
        CCRenderState.draw()
    }

    override def registerIcons(reg:IIconRegister)
    {
        bottom = reg.registerIcon("relocation:motor/bottom")
        top = reg.registerIcon("relocation:motor/top")
        side = reg.registerIcon("relocation:motor/side")
        sidew = reg.registerIcon("relocation:motor/sidew")
        sidee = reg.registerIcon("relocation:motor/sidee")

        iconT = new MultiIconTransformation(bottom, top, side, side, sidew, sidee)
    }
}

object FrameRenderer extends TInstancedBlockRender
{
    var icon:IIcon = _
    var iconT:UVTransformation = _

    override def renderWorldBlock(r:RenderBlocks, w:IBlockAccess, x:Int, y:Int, z:Int, meta:Int)
    {
        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.lightMatrix.locate(w, x, y, z)
        CCRenderState.setBrightness(w, x, y, z)
        RenderFrame.render(new Vector3(x, y, z))
    }

    override def getIcon(side:Int, meta:Int) = icon

    private val invVec = new Vector3(0, -0.075, 0)
    override def renderInvBlock(r:RenderBlocks, meta:Int)
    {
        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.setDynamic()
        CCRenderState.pullLightmap()

        CCRenderState.startDrawing()
        RenderFrame.render(invVec)

        CCRenderState.render()
        CCRenderState.draw()
    }

    override def registerIcons(reg:IIconRegister)
    {
        icon = reg.registerIcon("relocation:frame")
        iconT = new IconTransformation(icon)
    }
}

object RenderFrame
{
    val model = parseModel("frame")
    val model_simple = parseModel("frame_simple")

    def getFrameForRender = if (RelocationConfig.simpleModel) model_simple else model

    private def parseModel(name:String) =
    {
        val m = combine(parseObjModels(this.getClass.getResource(
            "/assets/relocation/textures/obj/"+name+".obj").openStream(),
            7, InvertX).values())

        m.shrinkUVs(0.0005)
        m.apply(new Scale(1.00075, 1.00075, 1.00075))
        m.apply(new Translation(Vector3.center))
        m.computeNormals
        m.computeLighting(LightModel.standardLightModel)
        m
    }

    def render(pos:Vector3)
    {
        getFrameForRender.render(new Translation(pos), FrameRenderer.iconT)
    }
}