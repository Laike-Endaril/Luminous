package com.fantasticsource.asmifiermod.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;

public class ASMifierTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (ASMifierCoremod.FULL_INPUT_CLASSNAMES.contains(transformedName))
        {
            try
            {
                InputStream in = new ByteArrayInputStream(bytes);
                ClassReader classReader = new ClassReader(in);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

                File file = new File("ASM Dumps" + File.separator + transformedName + "Dump.java");
                file.mkdirs();
                while (file.exists()) file.delete();

                System.out.println("====================================================================================");
                System.out.println("Outputting ASM dump: " + file.getAbsolutePath());
                System.out.println("====================================================================================");
                TraceClassVisitor traceClassVisitor = new TraceClassVisitor(cw, new ASMifier(), new PrintWriter(file));
                classReader.accept(traceClassVisitor, 0);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return bytes;
    }
}
