package com.fantasticsource.luminous.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

public class ClassMemberMapper extends ClassVisitor
{
    public static final HashMap<Integer, String> FIELD_ACCESS_FLAGS = new HashMap<>(), METHOD_ACCESS_FLAGS = new HashMap<>();

    static
    {
        FIELD_ACCESS_FLAGS.put(0x0001, "public");
        FIELD_ACCESS_FLAGS.put(0x0002, "private");
        FIELD_ACCESS_FLAGS.put(0x0004, "protected");
        FIELD_ACCESS_FLAGS.put(0x0008, "static");
        FIELD_ACCESS_FLAGS.put(0x0010, "final");
        FIELD_ACCESS_FLAGS.put(0x0040, "volatile");
        FIELD_ACCESS_FLAGS.put(0x0080, "transient");

        METHOD_ACCESS_FLAGS.put(0x0001, "public");
        METHOD_ACCESS_FLAGS.put(0x0002, "private");
        METHOD_ACCESS_FLAGS.put(0x0004, "protected");
        METHOD_ACCESS_FLAGS.put(0x0008, "static");
        METHOD_ACCESS_FLAGS.put(0x0010, "final");
        METHOD_ACCESS_FLAGS.put(0x0020, "synchronized");
        METHOD_ACCESS_FLAGS.put(0x0100, "native");
        METHOD_ACCESS_FLAGS.put(0x0400, "abstract");
        METHOD_ACCESS_FLAGS.put(0x0800, "strict");
    }


    public final HashMap<String, FieldData> FIELDS = new HashMap<>();
    public final HashMap<String, MethodData> METHODS = new HashMap<>();

    public ClassMemberMapper(int api, ClassVisitor cv)
    {
        super(api, cv);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        FIELDS.put(name, new FieldData(access, name, desc, signature, value));

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        METHODS.put(name, new MethodData(access, name, desc, signature, exceptions));

        return super.visitMethod(access, name, desc, signature, exceptions);
    }


    public static class FieldData
    {
        public int access;
        public String name, desc, signature;
        public Object value;

        public FieldData(int access, String name, String desc, String signature, Object value)
        {
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.value = value;
        }

        public String accessors()
        {
            StringBuilder s = new StringBuilder();
            for (Map.Entry<Integer, String> entry : FIELD_ACCESS_FLAGS.entrySet())
            {
                if ((access & entry.getKey()) != 0) s.append(entry.getValue()).append(" ");
            }
            return s.toString();
        }
    }

    public static class MethodData
    {
        public int access;
        public String name, desc, signature, exceptions[];

        public MethodData(int access, String name, String desc, String signature, String[] exceptions)
        {
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.exceptions = exceptions;
        }

        public String accessors()
        {
            StringBuilder s = new StringBuilder();
            for (Map.Entry<Integer, String> entry : METHOD_ACCESS_FLAGS.entrySet())
            {
                if ((access & entry.getKey()) != 0) s.append(entry.getValue()).append(" ");
            }
            return s.toString();
        }
    }
}
