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
    public static final TreeMap<String, TreeMap<String, String>> FIELD_TO_CODE = new TreeMap<>(Collections.reverseOrder()), SHORT_INNER_CLASS_TO_CODE = new TreeMap<>(Collections.reverseOrder());
    public static final TreeMap<String, TreeMap<String, HashMap<String, String>>> METHOD_TO_CODE = new TreeMap<>(Collections.reverseOrder());

    public static final HashMap<String, String> DEOBF_SUPERCLASSES = new HashMap<>();

    //TODO populate these
    public static final HashMap<String, HashMap<String, Boolean>> FIELD_IS_PRIVATE = new HashMap<>();
    public static final HashMap<String, HashMap<String, HashMap<String, Boolean>>> METHOD_IS_PRIVATE = new HashMap<>();


    public static void main(final String[] args) throws Exception
    {
        HashMap<File, String> deobfFiles = new HashMap<>();
        for (String className : args)
        {
            ClassReader cr;
            if (className.endsWith(".class") || className.indexOf('\\') > -1 || className.indexOf('/') > -1)
            {
                cr = new ClassReader(new FileInputStream(className));
            }
            else cr = new ClassReader(className);

            className = className.replaceAll("/", ".");

            File deobfFile = new File("deobf/" + className);
            cr.accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(deobfFile)), args.length == 2 ? 0 : ClassReader.SKIP_DEBUG);
            deobfFiles.put(deobfFile, className);
        }


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
                        String methodName = deobfMember.replaceFirst("[(].*[)]", ""), methodArgs = tokens[3].trim();
                        METHOD_TO_CODE.computeIfAbsent(currentDeobfClass, o -> new TreeMap<>(Collections.reverseOrder())).computeIfAbsent(methodName, o -> new HashMap<>()).put(methodArgs, cipher);
                        CODE_TO_OBF.put(cipher, tokens[0]);

                        boolean priv = false;
                        for (int i = 4; i < tokens.length; i++)
                        {
                            if (tokens[i].contains("private"))
                            {
                                priv = true;
                                break;
                            }
                        }
                        METHOD_IS_PRIVATE.computeIfAbsent(currentDeobfClass, o -> new HashMap<>()).computeIfAbsent(methodName, o -> new HashMap<>()).put(methodArgs, priv);
                    }
                    else
                    {
                        FIELD_TO_CODE.computeIfAbsent(currentDeobfClass, o -> new TreeMap<>(Collections.reverseOrder())).put(deobfMember, cipher);
                        CODE_TO_OBF.put(cipher, tokens[0]);

                        boolean priv = false;
                        for (int i = 3; i < tokens.length; i++)
                        {
                            if (tokens[i].contains("private"))
                            {
                                priv = true;
                                break;
                            }
                        }
                        FIELD_IS_PRIVATE.computeIfAbsent(currentDeobfClass, o -> new HashMap<>()).put(deobfMember, priv);
                    }
                }

                nextIsClass = false;
            }

            line = reader.readLine();
        }
        reader.close();


        for (Map.Entry<File, String> entry : deobfFiles.entrySet())
        {
            File deobfFile = entry.getKey();
            String className = entry.getValue();
            System.out.println(className + " ==========================================================================================");

            int i = 1;
            reader = new BufferedReader(new FileReader(deobfFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("obf/" + className)));
            line = reader.readLine();
            while (line != null)
            {
                while (line.contains("<init>")) line = line.replace("<init>", INIT_CODE);

                line = obfuscate(line, className.replaceAll("[.]", "/"), false); //Can debug a specific line here by setting "debug" param to i == <linenumber>
                if (line == null)
                {
                    writer.write("ERROR\r\n");
                    System.err.println("Line " + i);
                }
                else writer.write(line + "\r\n");

                System.out.print("\r" + i++ + " lines obfuscated           ");
                line = reader.readLine();
            }
            reader.close();
            writer.close();

            System.out.println("\n");
        }
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
        String originalLine = line;

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


        HashMap<String, String> fieldPool = new HashMap<>(), shortInnerClassPool = new HashMap<>();
        HashMap<String, HashMap<String, String>> methodPool = new HashMap<>();
        HashSet<String> classesIncluded = new HashSet<>();
        for (String classFound : memberClassesFound)
        {
//            if (debug) System.out.println("Class: " + classFound);
            classesIncluded.add(classFound);
            for (Map.Entry<String, String> entry : FIELD_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobfMemberName = entry.getKey();
//                if (debug) System.out.println("Field: " + deobfMemberName);
                if (!classFound.equals(deobfMainClass) && FIELD_IS_PRIVATE.get(classFound).get(deobfMemberName)) continue; //Private access check
                if (!lineContainsWord(line, deobfMemberName)) continue;

                if (fieldPool.containsKey(deobfMemberName))
                {
                    if (!CODE_TO_OBF.get(fieldPool.get(deobfMemberName)).equals(CODE_TO_OBF.get(entry.getValue())))
                    {
                        System.err.println();
                        System.err.println("Field conflict: " + deobfMemberName);
                        System.err.println(originalLine);
                        for (String cls : classesIncluded)
                        {
                            TreeMap<String, String> map = FIELD_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                            if (map.containsKey(deobfMemberName))
                            {
                                if (!cls.equals(deobfMainClass) && FIELD_IS_PRIVATE.get(cls).get(deobfMemberName)) continue; //Private access check
                                System.err.println(cls + ": " + deobfMemberName + ", " + CODE_TO_OBF.get(map.get(deobfMemberName)));
                            }
                        }
                        return null;
                    }
                }
                else
                {
                    fieldPool.put(deobfMemberName, entry.getValue());
                }
            }

            for (Map.Entry<String, HashMap<String, String>> entry : METHOD_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobfMemberName = entry.getKey();
//                if (debug) System.out.println("Method: " + deobfMemberName);

                for (Map.Entry<String, String> entry2 : entry.getValue().entrySet())
                {
                    String methodArgs = entry2.getKey();
                    if (!classFound.equals(deobfMainClass) && METHOD_IS_PRIVATE.get(classFound).get(deobfMemberName).get(methodArgs)) continue; //Private access check
                    if (!originalLine.contains(methodArgs)) continue; //Check for method argument match, in case of overloaded methods
                    if (!lineContainsWord(line, deobfMemberName)) continue;

                    if (methodPool.containsKey(deobfMemberName) && methodPool.get(deobfMemberName).containsKey(methodArgs))
                    {
                        if (!CODE_TO_OBF.get(methodPool.get(deobfMemberName).get(methodArgs)).equals(CODE_TO_OBF.get(entry2.getValue())))
                        {
                            System.err.println();
                            System.err.println("Method conflict: " + deobfMemberName + methodArgs);
                            System.err.println(originalLine);
                            for (String cls : classesIncluded)
                            {
                                TreeMap<String, HashMap<String, String>> map = METHOD_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                                if (map.containsKey(deobfMemberName) && map.get(deobfMemberName).containsKey(methodArgs))
                                {
                                    if (!cls.equals(deobfMainClass) && METHOD_IS_PRIVATE.get(cls).get(deobfMemberName).get(methodArgs)) continue; //Private access check
                                    System.err.println(cls + ": " + deobfMemberName + methodArgs + ", " + CODE_TO_OBF.get(map.get(deobfMemberName).get(methodArgs)));
                                }
                            }
                            return null;
                        }
                    }
                    else
                    {
                        methodPool.put(deobfMemberName, entry.getValue());
                    }
                }
            }

            for (Map.Entry<String, String> entry : SHORT_INNER_CLASS_TO_CODE.getOrDefault(classFound, new TreeMap<>(Collections.reverseOrder())).entrySet())
            {
                String deobfMemberName = entry.getKey();
                if (!lineContainsWord(line, deobfMemberName)) continue;

                if (shortInnerClassPool.containsKey(deobfMemberName))
                {
                    if (!CODE_TO_OBF.get(shortInnerClassPool.get(deobfMemberName)).equals(CODE_TO_OBF.get(entry.getValue())))
                    {
                        System.err.println();
                        System.err.println("Inner class conflict: " + deobfMemberName);
                        System.err.println(originalLine);
                        for (String cls : classesIncluded)
                        {
                            TreeMap<String, String> map = SHORT_INNER_CLASS_TO_CODE.getOrDefault(cls, new TreeMap<>(Collections.reverseOrder()));
                            if (map.containsKey(deobfMemberName)) System.err.println(cls + ": " + deobfMemberName + ", " + CODE_TO_OBF.get(map.get(deobfMemberName)));
                        }
                        return null;
                    }
                }
                else
                {
                    shortInnerClassPool.put(deobfMemberName, entry.getValue());
                }
            }
        }


        if (line.contains("visitMethod"))
        {
            for (Map.Entry<String, HashMap<String, String>> entry : methodPool.entrySet())
            {
                String deobf = entry.getKey();
                for (Map.Entry<String, String> entry2 : entry.getValue().entrySet())
                {
                    //Don't believe this needs a check for the original line containing args, since that should've been done earlier
                    line = replaceWordsInLine(line, deobf, entry2.getValue(), flag);
                }
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


        //Change from middleman codes to obfuscated
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


        StringBuilder newLine = new StringBuilder(), section = new StringBuilder();
        boolean isSection = false;
        for (char c : line.toCharArray())
        {
            if (isSection)
            {
                if (c == '"')
                {
                    newLine.append(replaceWordsInSection(section.toString(), toFind, replacement, didChange)).append('"');
                    section = new StringBuilder();
                    isSection = false;
                }
                else section.append(c);
            }
            else
            {
                newLine.append(c);
                if (c == '"') isSection = true;
            }
        }


        return newLine.toString();
    }

    public static String replaceWordsInSection(String section, String toFind, String replacement, boolean[] didChange)
    {
        if (section.length() == 0) return section;


        StringBuilder newSection = new StringBuilder();

        StringBuilder word = new StringBuilder();
        StringBuilder notword = new StringBuilder();
        for (char c : section.toCharArray())
        {
            if (VALID_CHARS.contains("" + c))
            {
                word.append(c);
                if (!notword.toString().equals(""))
                {
                    newSection.append(notword);
                    notword = new StringBuilder();
                }
            }
            else
            {
                notword.append(c);
                if (!word.toString().equals(""))
                {
                    newSection.append(replaceWord(word.toString(), toFind, replacement, didChange));
                    word = new StringBuilder();
                }
            }
        }

        if (!word.toString().equals("")) newSection.append(replaceWord(word.toString(), toFind, replacement, didChange));
        else if (!notword.toString().equals("")) newSection.append(notword);


        return newSection.toString();
    }

    public static String replaceWord(String word, String toFind, String replacement, boolean[] didChange)
    {
        if (word.contains("L" + toFind))
        {
            if (!word.equals("L" + toFind)) System.out.println("\r\nCheck recognition (line, toFind) (does not matter for never-obf entries eg. forge stuff): " + word + ", " + toFind);
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
