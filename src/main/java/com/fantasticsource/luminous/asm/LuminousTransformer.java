package com.fantasticsource.luminous.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class LuminousTransformer implements IClassTransformer
{
    private static final ArrayList<String> CLASSES_TO_TRANSFORM = new ArrayList<>();

    static
    {
        CLASSES_TO_TRANSFORM.add("net.minecraft.world.World");
        CLASSES_TO_TRANSFORM.add("amu");
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] classBytes)
    {
        if (CLASSES_TO_TRANSFORM.contains(name)) return transform(name, classBytes);
        return classBytes;
    }


    private static byte[] transform(String name, byte[] classBytes)
    {
        System.out.println("Transforming " + name);
        try
        {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(classNode, 0);


            //When adding new instructions to existing methods, it's best to add them at the start if possible to avoid frame-related issues
            switch (name)
            {
                case "net.minecraft.world.World":
                case "amu":
                    for (MethodNode methodNode : classNode.methods)
                    {
                        for (AbstractInsnNode instructionNode : methodNode.instructions.toArray())
                        {
                            if (instructionNode instanceof MethodInsnNode)
                            {
                                MethodInsnNode methodInstruction = (MethodInsnNode) instructionNode;
                                //When in obfuscated mode (normal run conditions), one class can have a ton of methods named "a", so use desc instead
                                if (methodInstruction.desc.equals("(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/EnumSkyBlock;)I") || methodInstruction.desc.equals("(Let;Lana;)I")) //getRawLight
                                {
                                    methodNode.instructions.set(instructionNode, new MethodInsnNode(INVOKESTATIC, "com/fantasticsource/luminous/Test1", "worldGetRawLightRedirect", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/EnumSkyBlock;)I", false));
                                }
                            }
                        }
                    }
                    break;
            }


            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS); //COMPUTE_FRAMES has major issues and should not be used; any necessary frame alterations should be handled manually (unfortunately)
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return classBytes;
    }
}
