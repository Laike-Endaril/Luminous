package com.fantasticsource.luminous.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ChunkClassVisitor extends ClassVisitor
{
    public ChunkClassVisitor(int api, ClassVisitor cv)
    {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        System.out.println(name);

        return mv;
    }
}
