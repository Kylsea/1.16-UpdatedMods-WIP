/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Auxiliary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.classloading.FMLForgePlugin;

import Reika.DragonAPI.Libraries.Java.ReikaASMHelper;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;

@SortingIndex(1001)
@MCVersion("1.7.10")
public class RotaryASMHandler implements IFMLLoadingPlugin {


	@Override
	public String[] getASMTransformerClass() {
		return new String[]{
				ASMExecutor.class.getName(),
				InterfaceVerifier.class.getName(),
				//ModelRedirector.class.getName(),
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {

	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	public static class ModelRedirector implements IClassTransformer {

		@Override
		public byte[] transform(String name, String transformedName, byte[] data) {
			if (data == null)
				return null;
			ClassNode cn = new ClassNode();
			ClassReader classReader = new ClassReader(data);
			classReader.accept(cn, 0);
			if (cn.name.startsWith("Reika/RotaryCraft/Models") && !cn.name.startsWith("Reika/RotaryCraft/Models/Animated")) {
				MethodNode m = ReikaASMHelper.getMethodByName(cn, "renderAll", "(Lnet/minecraft/tileentity/TileEntity;Ljava/util/ArrayList;FF)V");
				for (int i = 0; i < m.instructions.size(); i++) {
					AbstractInsnNode ain = m.instructions.get(i);
					if (ain.getOpcode() == Opcodes.INVOKESTATIC && ((MethodInsnNode)ain).name.startsWith("gl")) { //animation code

					}
				}
			}

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS/* | ClassWriter.COMPUTE_FRAMES*/);
			cn.accept(writer);
			return writer.toByteArray();
		}

	}

	public static class InterfaceVerifier implements IClassTransformer {

		private final String par1 = "Reika/RotaryCraft/TileEntities";
		private final String par2 = "Reika/RotaryCraft/Base/TileEntity";
		private final String par3 = "Reika/RotaryCraft/ModInterface";

		private final String rf1 = "cofh/api/energy/IEnergyHandler";
		private final String rf2 = "cofh/api/energy/IEnergyReceiver";

		private final HashSet<String> set = new HashSet();

		{
			set.add("Reika/RotaryCraft/ModInterface/Conversion/TileEntityDynamo");
			set.add("Reika/RotaryCraft/ModInterface/Conversion/TileEntityMagnetic");
		}

		@Override
		public byte[] transform(String name, String transformedName, byte[] data) {
			if (data == null)
				return null;
			ClassNode cn = new ClassNode();
			ClassReader classReader = new ClassReader(data);
			classReader.accept(cn, 0);
			if ((cn.name.contains(par1) || cn.name.contains(par2) || cn.name.contains(par3)) && !set.contains(cn.name)) {
				if (cn.interfaces.contains(rf1) || cn.interfaces.contains(rf2)) {
					cn.interfaces.remove(rf1);
					cn.interfaces.remove(rf2);
				}
			}

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS/* | ClassWriter.COMPUTE_FRAMES*/);
			cn.accept(writer);
			return writer.toByteArray();
		}

	}

	public static class ASMExecutor implements IClassTransformer {

		private static final HashMap<String, ClassPatch> classes = new HashMap();

		private static enum ClassPatch {
			FURNACEINTERCEPT("net.minecraft.tileentity.TileEntityFurnace", "apg"),
			;

			private final String obfName;
			private final String deobfName;

			private static final ClassPatch[] list = values();

			private ClassPatch(String name) {
				this(name, name);
			}

			private ClassPatch(String deobf, String obf) {
				obfName = obf;
				deobfName = deobf;
			}

			private byte[] apply(byte[] data) {
				ClassNode cn = new ClassNode();
				ClassReader classReader = new ClassReader(data);
				classReader.accept(cn, 0);
				int flags = ClassWriter.COMPUTE_MAXS;
				switch(this) {
					case FURNACEINTERCEPT: //TileEntityFurnaceHeater.isHijacked(te)
						InsnList pre = new InsnList();
						LabelNode L1 = new LabelNode();
						LabelNode L2 = new LabelNode();
						pre.add(new VarInsnNode(Opcodes.ALOAD, 0));
						pre.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Reika/RotaryCraft/TileEntities/Auxiliary/TileEntityFurnaceHeater", "isHijacked", "(Lnet/minecraft/tileentity/TileEntityFurnace;)Z", false));
						pre.add(new JumpInsnNode(Opcodes.IFEQ, L1));
						pre.add(L2);
						pre.add(new InsnNode(Opcodes.RETURN));
						pre.add(L1);

						MethodNode m = ReikaASMHelper.getMethodByName(cn, "func_145845_h", "updateEntity", "()V");
						m.instructions.insert(pre);

						flags |= ClassWriter.COMPUTE_FRAMES;
						break;
				}

				ClassWriter writer = new ClassWriter(flags);
				cn.accept(writer);
				return writer.toByteArray();
			}
		}

		@Override
		public byte[] transform(String className, String className2, byte[] opcodes) {
			if (!classes.isEmpty()) {
				ClassPatch p = classes.get(className);
				if (p != null) {
					ReikaASMHelper.activeMod = "RotaryCraft";
					ReikaASMHelper.log("Patching class "+p.deobfName);
					opcodes = p.apply(opcodes);
					classes.remove(className); //for maximizing performance
					ReikaASMHelper.activeMod = null;
				}
			}
			return opcodes;
		}

		static {
			for (int i = 0; i < ClassPatch.list.length; i++) {
				ClassPatch p = ClassPatch.list[i];
				String s = !FMLForgePlugin.RUNTIME_DEOBF ? p.deobfName : p.obfName;
				classes.put(s, p);
			}
		}
	}

}
