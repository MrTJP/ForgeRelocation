/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.relocation.asm

import java.util.{Map => JMap}

import mrtjp.relocation.handler.RelocationMod
import net.minecraft.launchwrapper.{IClassTransformer, Launch}
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.{INSTANCE => mapper}
import net.minecraftforge.fml.relauncher.{IFMLCallHook, IFMLLoadingPlugin}
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.tree._
import org.objectweb.asm.{ClassReader, ClassWriter}

import scala.collection.JavaConversions._

/**
  * Add "Dfml.coreMods.load=mrtjp.relocation.asm.RelocationPlugin" to launch configs
  * to enable in a development workspace.
  */
@IFMLLoadingPlugin.TransformerExclusions(value = Array("mrtjp.relocation.asm", "scala"))
class RelocationPlugin extends IFMLLoadingPlugin with IFMLCallHook {
  override def getASMTransformerClass = Array("mrtjp.relocation.asm.Transformer")
  override def getSetupClass = "mrtjp.relocation.asm.RelocationPlugin"
  override def getModContainerClass: Null = null
  override def getAccessTransformerClass: Null = null
  override def injectData(data: JMap[String, AnyRef]) {}
  override def call(): Void = null
}

class Transformer extends IClassTransformer {
  type MethodChecker = (String, MethodNode) => Boolean
  type InsTransformer = MethodNode => Unit

  lazy val deobfEnv: Boolean = (Launch.blackboard get "fml.deobfuscatedEnvironment").asInstanceOf[Boolean]
  lazy val blockClass: String = mapper.unmap("net/minecraft/block/Block")
  lazy val teClass: String = mapper.unmap("net/minecraft/tileentity/TileEntity")

//  def transformBlockRender(m: MethodNode) {
//    val old = m.instructions.toArray.collectFirst { case i: MethodInsnNode => i }.get
//    val list = new InsnList
//    list.add(new VarInsnNode(ALOAD, 2))
//    list.add(new MethodInsnNode(INVOKESTATIC, "mrtjp/relocation/ASMHacks",
//      "getRenderType", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/EnumBlockRenderType;", false))
//    m.instructions.insert(old, list)
//    m.instructions.remove(old)
//  }

  def transformTERender(m: MethodNode) {
    val insns = new InsnList
    List(
      new VarInsnNode(ALOAD, 1),
      new VarInsnNode(DLOAD, 2),
      new VarInsnNode(DLOAD, 4),
      new VarInsnNode(DLOAD, 6),
      new VarInsnNode(FLOAD, 8),
      new MethodInsnNode(INVOKESTATIC, "mrtjp/relocation/ASMHacks", "getTERenderPosition", "(Lnet/minecraft/tileentity/TileEntity;DDDF)Lcodechicken/lib/vec/Vector3;", false),
      new InsnNode(DUP),
      new InsnNode(DUP),
      new FieldInsnNode(GETFIELD, "codechicken/lib/vec/Vector3", "x", "D"),
      new VarInsnNode(DSTORE, 2),
      new FieldInsnNode(GETFIELD, "codechicken/lib/vec/Vector3", "y", "D"),
      new VarInsnNode(DSTORE, 4),
      new FieldInsnNode(GETFIELD, "codechicken/lib/vec/Vector3", "z", "D"),
      new VarInsnNode(DSTORE, 6)
    ).foreach(insns.add)
    m.instructions.insert(insns)
  }

  val classData = Map[String, (MethodChecker, MethodChecker, InsTransformer)](
//    "net.minecraft.client.renderer.BlockRendererDispatcher" -> ((
//      (_: String, m: MethodNode) => m.name == "renderBlock",
//      (n: String, m: MethodNode) => mapper.mapMethodName(n, m.name, m.desc) == "func_175018_a",
//      transformBlockRender
//    )),
    "net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher" -> ((
      (_: String, m: MethodNode) => m.name == "render" && m.desc == "(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V",
      (n: String, m: MethodNode) => mapper.mapMethodName(n, m.name, m.desc) == "func_192854_a",
      transformTERender
    ))
  )

  var matched: Set[String] = Set[String]()

  override def transform(name: String, tName: String, data: Array[Byte]): Array[Byte] = {
    if (classData.keys.contains(tName) && !matched.contains(tName)) {
      RelocationMod.log.info(s"transforming: $tName")
      val (ch1, ch2, tr) = classData(tName)

      val node = new ClassNode
      val reader = new ClassReader(data)
      reader.accept(node, 0)

      for (m <- node.methods)
        if ((deobfEnv && ch1(name, m)) || (!deobfEnv && ch2(name, m))) {
          RelocationMod.log.info(s"$name $tName ${m.name} ${m.desc}")
          tr(m)
        }

      matched += tName

      val writer = new ClassWriter(ClassWriter.COMPUTE_MAXS)
      node.accept(writer)
      writer.toByteArray
    }
    else data
  }
}