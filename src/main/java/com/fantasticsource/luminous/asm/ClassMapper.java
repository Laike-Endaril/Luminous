package com.fantasticsource.luminous.asm;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClassMapper
{
    public static final HashMap<String, String> CLASS_MAPPINGS = new HashMap<>(), FIELD_MAPPINGS = new HashMap<>(), METHOD_MAPPINGS = new HashMap<>();
    public static final HashMap<String, ClassMemberMapper> CLASS_MEMBER_MAPPERS = new HashMap<>();

    public static void put(String obfName, String deobfName, ClassMemberMapper classMemberMapper)
    {
        CLASS_MAPPINGS.put(obfName, deobfName);
        CLASS_MEMBER_MAPPERS.put(obfName, classMemberMapper);
    }

    public static void save()
    {
        try
        {
            File file = new File("E:\\Minecraft Modding\\~Mappings\\fields.csv");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null)
            {
                String[] tokens = line.split(",");
                FIELD_MAPPINGS.put(tokens[0], tokens[1]);
                line = reader.readLine();
            }

            file = new File("E:\\Minecraft Modding\\~Mappings\\methods.csv");
            reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
            while (line != null)
            {
                String[] tokens = line.split(",");
                METHOD_MAPPINGS.put(tokens[0], tokens[1]);
                line = reader.readLine();
            }


            file = new File("E:\\Minecraft Modding\\~Mappings\\classes.csv");
            file.mkdirs();
            file.delete();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<String, String> entry : CLASS_MAPPINGS.entrySet())
            {
                writer.write(entry.getKey() + ", " + entry.getValue() + "\r\n");
            }
            writer.close();


            file = new File("E:\\Minecraft Modding\\~Mappings\\all.csv");
            file.mkdirs();
            file.delete();
            BufferedWriter allMappingsWriter = new BufferedWriter(new FileWriter(file));
            String deobfClassname, translatedMember, translatedType, translatedArgTypes;
            for (Map.Entry<String, ClassMemberMapper> entry : CLASS_MEMBER_MAPPERS.entrySet())
            {
                String obfClassname = entry.getKey();
                ClassMemberMapper mapper = entry.getValue();

                deobfClassname = CLASS_MAPPINGS.get(obfClassname);
                allMappingsWriter.write(obfClassname + ", " + deobfClassname + "\r\n");
                if (mapper.superclass != null && !mapper.superclass.trim().equals("")) allMappingsWriter.write("extends, " + mapper.superclass + "\r\n");
                if (mapper.interfaces != null)
                {
                    for (String iface : mapper.interfaces) allMappingsWriter.write("implements, " + iface + "\r\n");
                }

                for (ClassMemberMapper.FieldData data : mapper.FIELDS.values())
                {
                    translatedMember = FIELD_MAPPINGS.getOrDefault(data.name, data.name);
                    translatedType = translateTypes(data.desc);
                    allMappingsWriter.write(data.name + ", " + deobfClassname + "." + translatedMember + ", " + translatedType + "\r\n");
                }

                for (ClassMemberMapper.MethodData data : mapper.METHODS.values())
                {
                    translatedMember = METHOD_MAPPINGS.getOrDefault(data.name, data.name);
                    translatedType = translateTypes(data.desc).replaceFirst("([(].*[)])", "");
                    translatedArgTypes = translateTypes(data.desc).replaceFirst("([(].*[)]).*", "$1");
                    allMappingsWriter.write(data.name + ", " + deobfClassname + "." + translatedMember + translatedArgTypes + ", " + translatedType + "\r\n");
                }

                allMappingsWriter.write("\r\n");
            }
            allMappingsWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String translateTypes(String rawTypes)
    {
        StringBuilder translated = new StringBuilder();

        boolean lastWasType = false;
        int i = 0;
        while (!rawTypes.equals(""))
        {
            if (i++ > 100)
            {
                System.out.println(rawTypes);
                System.exit(444);
            }

            char c = rawTypes.charAt(0);
            switch (c)
            {
                case 'V':
                    if (lastWasType) translated.append(" ");
                    translated.append("void");
                    lastWasType = true;
                    break;

                case 'Z':
                    if (lastWasType) translated.append(" ");
                    translated.append("boolean");
                    lastWasType = true;
                    break;

                case 'B':
                    if (lastWasType) translated.append(" ");
                    translated.append("byte");
                    lastWasType = true;
                    break;

                case 'C':
                    if (lastWasType) translated.append(" ");
                    translated.append("char");
                    lastWasType = true;
                    break;

                case 'S':
                    if (lastWasType) translated.append(" ");
                    translated.append("short");
                    lastWasType = true;
                    break;

                case 'I':
                    if (lastWasType) translated.append(" ");
                    translated.append("int");
                    lastWasType = true;
                    break;

                case 'J':
                    if (lastWasType) translated.append(" ");
                    translated.append("int");
                    lastWasType = true;
                    break;

                case 'F':
                    if (lastWasType) translated.append(" ");
                    translated.append("float");
                    lastWasType = true;
                    break;

                case 'D':
                    if (lastWasType) translated.append(" ");
                    translated.append("double");
                    lastWasType = true;
                    break;

                case 'L':
                    if (lastWasType) translated.append(" ");
                    translated.append(rawTypes.split(";")[0].replaceFirst("L", "").replaceAll("/", "."));
                    lastWasType = true;
                    break;

                default:
                    lastWasType = false;
                    translated.append(c);
            }

            if (c == 'L') rawTypes = rawTypes.replaceFirst("[^;]*;", "");
            else rawTypes = rawTypes.substring(1);
        }

        return translated.toString();
    }
}
