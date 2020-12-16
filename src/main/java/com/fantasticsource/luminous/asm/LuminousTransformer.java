package com.fantasticsource.luminous.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LuminousTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        switch (transformedName)
        {
            case "net.minecraft.world.World":
                //TODO
                break;

            case "net.minecraft.world.chunk.Chunk":
                //TODO
                break;

            case "net.minecraft.block.state.BlockStateContainer":
                //TODO
                break;
        }


        try
        {
            InputStream in = new ByteArrayInputStream(bytes);
            ClassReader classReader = new ClassReader(in);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            ClassTransformer classTransformer = new ClassTransformer(Opcodes.ASM5, cw);
            classReader.accept(classTransformer, 0);

            return cw.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(7777);
            return bytes;
        }
    }
}
