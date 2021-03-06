package com.fantasticsource.luminous.asm.classtransformers;

import org.objectweb.asm.*;

public class BlockStateContainerDump implements Opcodes
{
    public static byte[] dump()
    {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "awu", null, "java/lang/Object", null);

        cw.visitInnerClass("net/minecraft/block/state/BlockStateContainer$Builder", "awu", "Builder", ACC_PUBLIC + ACC_STATIC);

        cw.visitInnerClass("awu$a", "awu", "a", ACC_PUBLIC + ACC_STATIC);

        cw.visitInnerClass("awu$1", null, null, ACC_STATIC);

        cw.visitInnerClass("com/google/common/base/MoreObjects$ToStringHelper", "com/google/common/base/MoreObjects", "ToStringHelper", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "field_185921_a", "Ljava/util/regex/Pattern;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "field_177626_b", "Lcom/google/common/base/Function;", "Lcom/google/common/base/Function<Laxj<*>;Ljava/lang/String;>;", null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "field_177627_c", "Laow;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;", "Lcom/google/common/collect/ImmutableSortedMap<Ljava/lang/String;Laxj<*>;>;", null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "field_177625_e", "Lcom/google/common/collect/ImmutableList;", "Lcom/google/common/collect/ImmutableList<Lawt;>;", null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "<init>", "(Laow;[Laxj;)V", "(Laow;[Laxj<*>;)V", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, "awu", "<init>", "(Laow;[Laxj;Lcom/google/common/collect/ImmutableMap;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "createState", "(Laow;Lcom/google/common/collect/ImmutableMap;Lcom/google/common/collect/ImmutableMap;)Lawu$a;", "(Laow;Lcom/google/common/collect/ImmutableMap<Laxj<*>;Ljava/lang/Comparable<*>;>;Lcom/google/common/collect/ImmutableMap<Lnet/minecraftforge/common/property/IUnlistedProperty<*>;Ljava/util/Optional<*>;>;)Lawu$a;", null);
            {
                av0 = mv.visitParameterAnnotation(2, "Ljavax/annotation/Nullable;", true);
                av0.visitEnd();
            }
            mv.visitCode();
            mv.visitTypeInsn(NEW, "awu$a");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "awu$a", "<init>", "(Laow;Lcom/google/common/collect/ImmutableMap;)V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "<init>", "(Laow;[Laxj;Lcom/google/common/collect/ImmutableMap;)V", "(Laow;[Laxj<*>;Lcom/google/common/collect/ImmutableMap<Lnet/minecraftforge/common/property/IUnlistedProperty<*>;Ljava/util/Optional<*>;>;)V", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, "awu", "field_177627_c", "Laow;");
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/Maps", "newHashMap", "()Ljava/util/HashMap;", false);
            mv.visitVarInsn(ASTORE, 4);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ASTORE, 5);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitVarInsn(ISTORE, 6);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 7);
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_FULL, 8, new Object[]{"awu", "aow", "[Laxj;", "com/google/common/collect/ImmutableMap", "java/util/Map", "[Laxj;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
            mv.visitVarInsn(ILOAD, 7);
            mv.visitVarInsn(ILOAD, 6);
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l1);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ASTORE, 8);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitMethodInsn(INVOKESTATIC, "awu", "func_185919_a", "(Laow;Laxj;)Ljava/lang/String;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitMethodInsn(INVOKEINTERFACE, "axj", "func_177701_a", "()Ljava/lang/String;", true); //TODO When autogenerated, this line incorrectly had func_177702_a; another overload of IProperty.getName()
            mv.visitVarInsn(ALOAD, 8);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            mv.visitIincInsn(7, 1);
            mv.visitJumpInsn(GOTO, l0);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/ImmutableSortedMap", "copyOf", "(Ljava/util/Map;)Lcom/google/common/collect/ImmutableSortedMap;", false);
            mv.visitFieldInsn(PUTFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/Maps", "newLinkedHashMap", "()Ljava/util/LinkedHashMap;", false);
            mv.visitVarInsn(ASTORE, 5);
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/Lists", "newArrayList", "()Ljava/util/ArrayList;", false);
            mv.visitVarInsn(ASTORE, 6);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "awu", "func_177620_e", "()Ljava/util/List;", false);
            mv.visitMethodInsn(INVOKESTATIC, "ew", "func_179321_a", "(Ljava/lang/Iterable;)Ljava/lang/Iterable;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Iterable", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitVarInsn(ASTORE, 7);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"java/util/Map", "java/util/List", "java/util/Iterator"}, 0, null);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/util/List");
            mv.visitVarInsn(ASTORE, 8);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableSortedMap", "values", "()Lcom/google/common/collect/ImmutableCollection;", false);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitMethodInsn(INVOKESTATIC, "fg", "func_179400_b", "(Ljava/lang/Iterable;Ljava/lang/Iterable;)Ljava/util/Map;", false);
            mv.visitVarInsn(ASTORE, 9);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/ImmutableMap", "copyOf", "(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;", false);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "awu", "createState", "(Laow;Lcom/google/common/collect/ImmutableMap;Lcom/google/common/collect/ImmutableMap;)Lawu$a;", false);
            mv.visitVarInsn(ASTORE, 10);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitVarInsn(ALOAD, 10);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitVarInsn(ALOAD, 10);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            mv.visitJumpInsn(GOTO, l2);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitVarInsn(ASTORE, 7);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/util/Iterator"}, 0, null);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            Label l5 = new Label();
            mv.visitJumpInsn(IFEQ, l5);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "awu$a");
            mv.visitVarInsn(ASTORE, 8);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "awu$a", "func_177235_a", "(Ljava/util/Map;)V", false);
            mv.visitJumpInsn(GOTO, l4);
            mv.visitLabel(l5);
            mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/ImmutableList", "copyOf", "(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;", false);
            mv.visitFieldInsn(PUTFIELD, "awu", "field_177625_e", "Lcom/google/common/collect/ImmutableList;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 11);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "func_185919_a", "(Laow;Laxj;)Ljava/lang/String;", "<T::Ljava/lang/Comparable<TT;>;>(Laow;Laxj<TT;>;)Ljava/lang/String;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "axj", "func_177701_a", "()Ljava/lang/String;", true); //TODO When autogenerated, this line incorrectly had func_177702_a; another overload of IProperty.getName()
            mv.visitVarInsn(ASTORE, 2);
            mv.visitFieldInsn(GETSTATIC, "awu", "field_185921_a", "Ljava/util/regex/Pattern;");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
            Label l0 = new Label();
            mv.visitJumpInsn(IFNE, l0);
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Block: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(" has invalidly named property: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "axj", "func_177700_c", "()Ljava/util/Collection;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitVarInsn(ASTORE, 3);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/util/Iterator"}, 0, null);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            Label l2 = new Label();
            mv.visitJumpInsn(IFEQ, l2);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/Comparable");
            mv.visitVarInsn(ASTORE, 4);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKEINTERFACE, "axj", "func_177702_a", "(Ljava/lang/Comparable;)Ljava/lang/String;", true);
            mv.visitVarInsn(ASTORE, 5);
            mv.visitFieldInsn(GETSTATIC, "awu", "field_185921_a", "Ljava/util/regex/Pattern;");
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
            Label l3 = new Label();
            mv.visitJumpInsn(IFNE, l3);
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Block: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(" has property: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(" with invalidly named value: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitJumpInsn(GOTO, l1);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 6);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "func_177619_a", "()Lcom/google/common/collect/ImmutableList;", "()Lcom/google/common/collect/ImmutableList<Lawt;>;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177625_e", "Lcom/google/common/collect/ImmutableList;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE, "func_177620_e", "()Ljava/util/List;", "()Ljava/util/List<Ljava/lang/Iterable<Ljava/lang/Comparable<*>;>;>;", null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/Lists", "newArrayList", "()Ljava/util/ArrayList;", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableSortedMap", "values", "()Lcom/google/common/collect/ImmutableCollection;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableCollection", "iterator", "()Lcom/google/common/collect/UnmodifiableIterator;", false);
            mv.visitVarInsn(ASTORE, 3);
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"java/util/List", "com/google/common/collect/ImmutableCollection", "com/google/common/collect/UnmodifiableIterator"}, 0, null);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/UnmodifiableIterator", "hasNext", "()Z", false);
            Label l1 = new Label();
            mv.visitJumpInsn(IFEQ, l1);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/UnmodifiableIterator", "next", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "axj");
            mv.visitVarInsn(ASTORE, 4);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKEINTERFACE, "axj", "func_177700_c", "()Ljava/util/Collection;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            mv.visitJumpInsn(GOTO, l0);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "func_177621_b", "()Lawt;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177625_e", "Lcom/google/common/collect/ImmutableList;");
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableList", "get", "(I)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "awt");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "func_177622_c", "()Laow;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177627_c", "Laow;");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "func_177623_d", "()Ljava/util/Collection;", "()Ljava/util/Collection<Laxj<*>;>;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableSortedMap", "values", "()Lcom/google/common/collect/ImmutableCollection;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/base/MoreObjects", "toStringHelper", "(Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;", false);
            mv.visitLdcInsn("block");
            mv.visitFieldInsn(GETSTATIC, "aow", "field_149771_c", "Ley;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177627_c", "Laow;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "ey", "func_177774_c", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/base/MoreObjects$ToStringHelper", "add", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;", false);
            mv.visitLdcInsn("properties");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableSortedMap", "values", "()Lcom/google/common/collect/ImmutableCollection;", false);
            mv.visitFieldInsn(GETSTATIC, "awu", "field_177626_b", "Lcom/google/common/base/Function;");
            mv.visitMethodInsn(INVOKESTATIC, "com/google/common/collect/Iterables", "transform", "(Ljava/lang/Iterable;Lcom/google/common/base/Function;)Ljava/lang/Iterable;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/base/MoreObjects$ToStringHelper", "add", "(Ljava/lang/String;Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/base/MoreObjects$ToStringHelper", "toString", "()Ljava/lang/String;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "func_185920_a", "(Ljava/lang/String;)Laxj;", "(Ljava/lang/String;)Laxj<*>;", null);
            {
                av0 = mv.visitAnnotation("Ljavax/annotation/Nullable;", true);
                av0.visitEnd();
            }
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "awu", "field_177624_d", "Lcom/google/common/collect/ImmutableSortedMap;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/google/common/collect/ImmutableSortedMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "axj");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitLdcInsn("^[a-z0-9_]+$");
            mv.visitMethodInsn(INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
            mv.visitFieldInsn(PUTSTATIC, "awu", "field_185921_a", "Ljava/util/regex/Pattern;");
            mv.visitTypeInsn(NEW, "awu$1");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "awu$1", "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, "awu", "field_177626_b", "Lcom/google/common/base/Function;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }
}
