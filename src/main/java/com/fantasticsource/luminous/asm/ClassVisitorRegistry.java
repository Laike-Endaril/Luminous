package com.fantasticsource.luminous.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class ClassVisitorRegistry implements IClassTransformer
{
    //TODO Started making this flexible enough to be in a library coremod, but since I've had no luck running coremods in dev environment...
    //TODO Could either put it in a new library, or figure out how to run coremods in dev...but for now I'll leave it here

    protected static final HashMap<String, Constructor<ClassVisitor>> CLASS_VISITOR_CONSTRUCTORS = new HashMap<>();

    public static void register(String transformedClassName, Class<? extends ClassVisitor> visitorClass)
    {
        if (CLASS_VISITOR_CONSTRUCTORS.containsKey(transformedClassName)) throw new IllegalArgumentException("There was already a class visitor registered for class: " + transformedClassName);

        //Find the right constructor, if it exists
        Constructor<ClassVisitor> constructor = null;
        Constructor[] constructors = visitorClass.getConstructors();
        for (Constructor con : constructors)
        {
            Class[] paramTypes = con.getParameterTypes();
            for (Class c : paramTypes) System.out.println(c.getName());
            if (paramTypes.length == 2 && int.class.isAssignableFrom(paramTypes[0]) && ClassVisitor.class.isAssignableFrom(paramTypes[1]))
            {
                constructor = con;
                break;
            }
        }

        if (constructor == null) throw new IllegalArgumentException("No applicable constructor found for visitor class: " + visitorClass.getName() + " (arguments must be (int, ClassVisitor))");
        else CLASS_VISITOR_CONSTRUCTORS.put(transformedClassName, constructor);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        Constructor<ClassVisitor> constructor = CLASS_VISITOR_CONSTRUCTORS.get(transformedName);
        if (constructor == null) return bytes;


        System.out.println("Applying class visitor (" + constructor.getDeclaringClass().getName() + ") to class (" + transformedName + ")");
        try
        {
            InputStream in = new ByteArrayInputStream(bytes);
            ClassReader classReader = new ClassReader(in);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            classReader.accept(constructor.newInstance(Opcodes.ASM5, cw), 0);

            return cw.toByteArray();
        }
        catch (IOException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
            System.exit(7777);
            return bytes;
        }
    }
}
