package com.fantasticsource.luminous.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassMemberMapper extends ClassVisitor
{
    public static final HashMap<Integer, String> CLASS_ACCESS_FLAGS = new HashMap<>(), FIELD_ACCESS_FLAGS = new HashMap<>(), METHOD_ACCESS_FLAGS = new HashMap<>();

    static
    {
        CLASS_ACCESS_FLAGS.put(0x0001, "public");
        FIELD_ACCESS_FLAGS.put(0x0001, "public");
        METHOD_ACCESS_FLAGS.put(0x0001, "public");

        CLASS_ACCESS_FLAGS.put(0x0002, "private");
        FIELD_ACCESS_FLAGS.put(0x0002, "private");
        METHOD_ACCESS_FLAGS.put(0x0002, "private");

        CLASS_ACCESS_FLAGS.put(0x0004, "protected");
        FIELD_ACCESS_FLAGS.put(0x0004, "protected");
        METHOD_ACCESS_FLAGS.put(0x0004, "protected");

        FIELD_ACCESS_FLAGS.put(0x0008, "static");
        METHOD_ACCESS_FLAGS.put(0x0008, "static");

        CLASS_ACCESS_FLAGS.put(0x0010, "final");
        FIELD_ACCESS_FLAGS.put(0x0010, "final");
        METHOD_ACCESS_FLAGS.put(0x0010, "final");
        //Also parameter, if that comes into play

        CLASS_ACCESS_FLAGS.put(0x0020, "super");

        METHOD_ACCESS_FLAGS.put(0x0020, "synchronized");

        FIELD_ACCESS_FLAGS.put(0x0040, "volatile");

        METHOD_ACCESS_FLAGS.put(0x0040, "bridge");

        FIELD_ACCESS_FLAGS.put(0x0080, "transient");

        METHOD_ACCESS_FLAGS.put(0x0080, "varargs");

        METHOD_ACCESS_FLAGS.put(0x0100, "native");

        CLASS_ACCESS_FLAGS.put(0x0200, "interface");

        CLASS_ACCESS_FLAGS.put(0x0400, "abstract");
        METHOD_ACCESS_FLAGS.put(0x0400, "abstract");

        METHOD_ACCESS_FLAGS.put(0x0800, "strict");

        CLASS_ACCESS_FLAGS.put(0x1000, "synthetic");
        FIELD_ACCESS_FLAGS.put(0x1000, "synthetic");
        METHOD_ACCESS_FLAGS.put(0x1000, "synthetic");
        //Also parameter, if that comes into play

        CLASS_ACCESS_FLAGS.put(0x2000, "annotation");

        //ASM note was...
        // class(?) field inner
        //...and I wasn't sure how to translate that, so for now, since there is no numerical equivalent for any other access tag anyway...
        CLASS_ACCESS_FLAGS.put(0x4000, "enum");
        FIELD_ACCESS_FLAGS.put(0x4000, "enum");

//        0x8000, "mandated" // For parameter, if that comes into play
    }


    public String superclass, interfaces[];
    public ArrayList<String> accessors = new ArrayList<>();
    public final HashMap<String, FieldData> fields = new HashMap<>();
    public final HashMap<String, MethodData> methods = new HashMap<>();

    public ClassMemberMapper(int api, ClassVisitor cv)
    {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        this.superclass = superName;
        this.interfaces = interfaces;

        for (Map.Entry<Integer, String> entry : CLASS_ACCESS_FLAGS.entrySet())
        {
            if ((access & entry.getKey()) != 0) accessors.add(entry.getValue());
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        fields.put(name, new FieldData(access, name, desc, signature, value));

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        methods.put(name, new MethodData(access, name, desc, signature, exceptions));

        return super.visitMethod(access, name, desc, signature, exceptions);
    }


    public static class FieldData
    {
        public int access;
        public String name, desc, signature;
        public Object value;
        public ArrayList<String> accessors = new ArrayList<>();

        public FieldData(int access, String name, String desc, String signature, Object value)
        {
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.value = value;

            for (Map.Entry<Integer, String> entry : FIELD_ACCESS_FLAGS.entrySet())
            {
                if ((access & entry.getKey()) != 0) accessors.add(entry.getValue());
            }
        }
    }

    public static class MethodData
    {
        public int access;
        public String name, desc, signature, exceptions[];
        public ArrayList<String> accessors = new ArrayList<>();

        public MethodData(int access, String name, String desc, String signature, String[] exceptions)
        {
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.exceptions = exceptions;

            for (Map.Entry<Integer, String> entry : METHOD_ACCESS_FLAGS.entrySet())
            {
                if ((access & entry.getKey()) != 0) accessors.add(entry.getValue());
            }
        }
    }
}
