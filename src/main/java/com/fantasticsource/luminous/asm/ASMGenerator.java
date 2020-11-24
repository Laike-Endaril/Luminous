package com.fantasticsource.luminous.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.util.*;

public class ASMGenerator
{
    public static final String VALID_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz/$_";
    public static final char[] ENCODED_DIGITS = "!@#$%^&*()".toCharArray();
    public static final String INIT_CODE = "{{{^}}}";

    public static final HashMap<String, String> CODE_TO_OBF = new HashMap<>();

    static
    {
        CODE_TO_OBF.put(INIT_CODE, "<init>");
    }

    public static final TreeMap<String, String> UNOBF_CLASS_TO_CODE = new TreeMap<>(Collections.reverseOrder()), INNER_CLASS_TO_CODE = new TreeMap<>(Collections.reverseOrder()), OUTER_CLASS_TO_CODE = new TreeMap<>(Collections.reverseOrder());
    public static final TreeMap<String, TreeMap<String, String>> FIELD_TO_CODE = new TreeMap<>(Collections.reverseOrder()), METHOD_TO_CODE = new TreeMap<>(Collections.reverseOrder()), SHORT_INNER_CLASS_TO_CODE = new TreeMap<>(Collections.reverseOrder());
    public static final HashMap<String, String> DEOBF_SUPERCLASSES = new HashMap<>();

    public static void main(final String[] args) throws Exception
    {
        if (!(args.length == 1 || (args.length == 2 && "-debug".equals(args[0]))))
        {
            System.err.println("Prints the ASM code to generate the given class.");
            System.err.println("Usage: ASMifier [-debug] <fully qualified class name or class file name>");
            return;
        }


        String className = args[args.length - 1];
        ClassReader cr;
        if (className.endsWith(".class") || className.indexOf('\\') > -1 || className.indexOf('/') > -1)
        {
            cr = new ClassReader(new FileInputStream(className));
        }
        else cr = new ClassReader(className);

        className = className.replaceAll("/", ".");

        File deobfFile = new File("deobf/" + className);
        cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(deobfFile)), args.length == 2 ? 0 : ClassReader.SKIP_DEBUG);


        BufferedReader reader = new BufferedReader(new FileReader(new File("E:\\Minecraft Modding\\~Mappings\\all.csv")));
        String line = reader.readLine();
        boolean nextIsClass = true;
        String currentDeobfClass = null;
        int nextCode = 0;
        while (line != null)
        {
            String[] tokens = line.split(",");
            if (tokens.length < 2)
            {
                nextIsClass = true;
            }
            else
            {
                tokens[0] = tokens[0].replaceAll("[.]", "/");
                tokens[1] = tokens[1].trim().replaceAll("[.]", "/");

                if (nextIsClass)
                {
                    currentDeobfClass = tokens[1];
                    if (currentDeobfClass.equals(tokens[0]))
                    {
                        String cipher = cipher(nextCode++);
                        UNOBF_CLASS_TO_CODE.put(currentDeobfClass, cipher);
                        CODE_TO_OBF.put(cipher, currentDeobfClass);
                    }
                    else
                    {
                        if (currentDeobfClass.contains("$"))
                        {
                            String obf = tokens[0];
                            String cipher = cipher(nextCode++);
                            INNER_CLASS_TO_CODE.put(currentDeobfClass, cipher);
                            CODE_TO_OBF.put(cipher, obf);
                            cipher = cipher(nextCode++);
                            SHORT_INNER_CLASS_TO_CODE.computeIfAbsent(currentDeobfClass.substring(0, currentDeobfClass.indexOf('$')), o -> new TreeMap<>(Collections.reverseOrder())).put(currentDeobfClass.substring(currentDeobfClass.indexOf('$') + 1), cipher);
                            CODE_TO_OBF.put(cipher, obf.substring(obf.indexOf('$') + 1));
                        }
                        else
                        {
                            String cipher = cipher(nextCode++);
                            OUTER_CLASS_TO_CODE.put(currentDeobfClass, cipher);
                            CODE_TO_OBF.put(cipher, tokens[0]);
                        }
                    }
                }
                else if (tokens[0].equals("extends"))
                {
                    DEOBF_SUPERCLASSES.put(currentDeobfClass, tokens[1]);
                }
                else if (tokens[0].equals("implements"))
                {
                    //Don't need interface data, because implementations already override all interface methods
                }
                else
                {
                    String deobfMember = tokens[1].replace(currentDeobfClass + "/", "");
                    String cipher = cipher(nextCode++);

                    if (deobfMember.matches(".*[(].*[)].*"))
                    {
                        METHOD_TO_CODE.computeIfAbsent(currentDeobfClass, o -> new TreeMap<>(Collections.reverseOrder())).put(deobfMember.replaceFirst("[(].*[)]", ""), cipher);
                        CODE_TO_OBF.put(cipher, tokens[0]);
                    }
                    else
                    {
                        FIELD_TO_CODE.computeIfAbsent(currentDeobfClass, o -> new TreeMap<>(Collections.reverseOrder())).put(deobfMember, cipher);
                        CODE_TO_OBF.put(cipher, tokens[0]);
                    }
                }

                nextIsClass = false;
            }

            line = reader.readLine();
        }
        reader.close();


        int i = 0;
        reader = new BufferedReader(new FileReader(deobfFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("obf/" + className)));
        line = reader.readLine();
        while (line != null)
        {
            while (line.contains("<init>")) line = line.replace("<init>", INIT_CODE);

            if (++i % 100 == 0) System.out.println(i);
            line = obfuscate(line, className.replaceAll("[.]", "/"), false); //Can debug a specific line here by setting "debug" param to i == <linenumber>
            if (line == null)
            {
                writer.write("ERROR\r\n");
                System.err.println(i);
                System.err.println();
            }
            else writer.write(line + "\r\n");
            line = reader.readLine();
        }
        reader.close();
        writer.close();
    }


    public static String cipher(int code)
    {
        String s = "" + code;
        StringBuilder cipher = new StringBuilder("{?!?}");
        for (char c : s.toCharArray()) cipher.append(ENCODED_DIGITS[Integer.parseInt("" + c)]);
        cipher.append("{!?!}");
        return cipher.toString();
    }


    public static String obfuscate(String line, String deobfMainClass, boolean debug)
    {
        if (debug) System.out.println(line);
        String original = line;

        HashSet<String> memberClassesFound = new HashSet<>();
        HashSet<String> unobfClassesFound = new HashSet<>();

        boolean[] flag = new boolean[1];
        for (TreeMap<String, String> map : new TreeMap[]{UNOBF_CLASS_TO_CODE, INNER_CLASS_TO_CODE, OUTER_CLASS_TO_CODE})
        {
            for (Map.Entry<String, String> entry : map.entrySet())
            {
                String deobf = entry.getKey();
                String code = entry.getValue();
                line = replaceWordsInLine(line, deobf, code, flag);
                if (flag[0])
                {
                    if (!line.contains("visitMethod(") && (!line.contains("visitMethodInsn") || line.contains('"' + code)))
                    {
                        memberClassesFound.add(deobf);
                        String superclass = DEOBF_SUPERCLASSES.get(deobf);
                        while (superclass != null)
                        {
                            memberClassesFound.add(superclass);
                            superclass = DEOBF_SUPERCLASSES.get(superclass);
                        }
                    }
                    if (map == UNOBF_CLASS_TO_CODE) unobfClassesFound.add(deobf);
                }
            }
        }

        if (!line.contains("visitMethodInsn"))
        {
            memberClassesFound.add(deobfMainClass);
            String superclass = DEOBF_SUPERCLASSES.get(deobfMainClass);
            while (superclass != null)
            {
                memberClassesFound.add(superclass);
                superclass = DEOBF_SUPERCLASSES.get(superclass);
            }
        }


        HashMap<String, String> fieldPool = new HashMap<>(), methodPool = new HashMap<>(), shortInnerClassPool = new HashMap<>();
        HashSet<String> classesIncluded = new HashSet<>();
        for (String classFound : memberClassesFound)
        {
//            if (debug) System.out.println("Class: " + classFound);
            classesIncluded.add(classFound);
            for (Map.Entry<String, String> entry : FIELD_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobf = entry.getKey();
//                if (debug) System.out.println("Field: " + deobf);
                if (!lineContainsWord(line, deobf)) continue;

                if (fieldPool.containsKey(deobf))
                {
                    if (!CODE_TO_OBF.get(fieldPool.get(deobf)).equals(CODE_TO_OBF.get(entry.getValue())))
                    {
                        System.err.println();
                        System.err.println("Field conflict: " + deobf);
                        System.err.println(original);
                        for (String cls : classesIncluded)
                        {
                            TreeMap<String, String> map = FIELD_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                            if (map.containsKey(deobf)) System.err.println(cls + ": " + deobf + ", " + map.get(deobf) + ", " + CODE_TO_OBF.get(map.get(deobf)));
                        }
                        return null;
                    }
                }
                else
                {
                    fieldPool.put(deobf, entry.getValue());
                }
            }

            for (Map.Entry<String, String> entry : METHOD_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobf = entry.getKey();
//                if (debug) System.out.println("Method: " + deobf);
                if (!lineContainsWord(line, deobf)) continue;

                if (methodPool.containsKey(deobf))
                {
                    if (!CODE_TO_OBF.get(methodPool.get(deobf)).equals(CODE_TO_OBF.get(entry.getValue())))
                    {
                        System.err.println();
                        System.err.println("Method conflict: " + deobf);
                        System.err.println(original);
                        for (String cls : classesIncluded)
                        {
                            TreeMap<String, String> map = METHOD_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                            if (map.containsKey(deobf)) System.err.println(cls + ": " + deobf + ", " + map.get(deobf) + ", " + CODE_TO_OBF.get(map.get(deobf)));
                        }
                        return null;
                    }
                }
                else
                {
                    methodPool.put(deobf, entry.getValue());
                }
            }

            for (Map.Entry<String, String> entry : SHORT_INNER_CLASS_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobf = entry.getKey();
                if (!lineContainsWord(line, deobf)) continue;

                if (shortInnerClassPool.containsKey(deobf))
                {
                    if (!CODE_TO_OBF.get(shortInnerClassPool.get(deobf)).equals(CODE_TO_OBF.get(entry.getValue())))
                    {
                        System.err.println();
                        System.err.println("Inner class conflict: " + deobf);
                        System.err.println(original);
                        for (String cls : classesIncluded)
                        {
                            TreeMap<String, String> map = SHORT_INNER_CLASS_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                            if (map.containsKey(deobf)) System.err.println(cls + ": " + deobf + ", " + map.get(deobf) + ", " + CODE_TO_OBF.get(map.get(deobf)));
                        }
                        return null;
                    }
                }
                else
                {
                    shortInnerClassPool.put(deobf, entry.getValue());
                }
            }
        }

        if (line.contains("visitMethod"))
        {
            for (Map.Entry<String, String> entry : methodPool.entrySet())
            {
                String deobf = entry.getKey();
                line = replaceWordsInLine(line, deobf, entry.getValue(), flag);
            }
        }
        else
        {
            for (Map.Entry<String, String> entry : fieldPool.entrySet())
            {
                String deobf = entry.getKey();
                line = replaceWordsInLine(line, deobf, entry.getValue(), flag);
            }
        }

        for (Map.Entry<String, String> entry : shortInnerClassPool.entrySet())
        {
            String deobf = entry.getKey();
            line = replaceWordsInLine(line, deobf, entry.getValue(), flag);
        }


        for (String className : unobfClassesFound)
        {
            line = replaceWordsInLine(line, className, UNOBF_CLASS_TO_CODE.get(className), flag);
        }

        for (Map.Entry<String, String> entry : CODE_TO_OBF.entrySet())
        {
            String code = entry.getKey();
            while (line.contains(code)) line = line.replace(code, entry.getValue());
        }

        return line;
    }


    public static boolean lineContainsWord(String line, String word)
    {
        boolean[] result = new boolean[]{false};
        replaceWordsInLine(line, word, word + "*", result);
        return result[0];
    }

    public static String replaceWordsInLine(String line, String toFind, String replacement, boolean[] didChange)
    {
        didChange[0] = false;
        if (line.length() == 0) return line;


        StringBuilder newLine = new StringBuilder();

        StringBuilder word = new StringBuilder();
        StringBuilder notword = new StringBuilder();
        for (char c : line.toCharArray())
        {
            if (VALID_CHARS.contains("" + c))
            {
                word.append(c);
                if (!notword.toString().equals(""))
                {
                    newLine.append(notword);
                    notword = new StringBuilder();
                }
            }
            else
            {
                notword.append(c);
                if (!word.toString().equals(""))
                {
                    newLine.append(replaceWord(word.toString(), toFind, replacement, didChange));
                    word = new StringBuilder();
                }
            }
        }

        if (!word.toString().equals("")) newLine.append(replaceWord(word.toString(), toFind, replacement, didChange));
        else if (!notword.toString().equals("")) newLine.append(notword);


        return newLine.toString();
    }

    public static String replaceWord(String word, String toFind, String replacement, boolean[] didChange)
    {
        if (word.contains("L" + toFind))
        {
            if (!word.equals("L" + toFind)) System.out.println(word + ", " + toFind);
            didChange[0] = true;
            return word.replace("L" + toFind, "L" + replacement);
        }

        if (word.equals(toFind))
        {
            didChange[0] = true;
            return replacement;
        }

        return word;
    }
}
