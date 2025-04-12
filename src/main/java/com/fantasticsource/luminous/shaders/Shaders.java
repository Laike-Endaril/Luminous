package com.fantasticsource.luminous.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.fantasticsource.luminous.Luminous.MODID;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

public class Shaders
{
    public static final int
            NONE = 0,
            LIGHTS_ATTRIB_POSITION = 0,
            LIGHTS_ATTRIB_TEXTURE_COORDS = 1,
            LIGHTS_ATTRIB_FADE = 3,
            LIGHTS_ATTRIB_COLOR = 4,
            LIGHTS_ATTRIB_NORMALS = 5,
            LIGHTS_FRAGDATA_COLOR = 0,
            LIGHTS_VERT = createShader("shaders/lights.vert"),
            LIGHTS_FRAG = createShader("shaders/lights.frag"),
            LIGHTS = createShaderProgram("lights"),
            LIGHTS_UNIFORM_MVP = glGetUniformLocation(LIGHTS, "uniform_mvp"),
            LIGHTS_UNIFORM_TEXTURE_SAMPLER = glGetUniformLocation(LIGHTS, "uniform_sampler");


    public static void init()
    {
        //Calling this method indirectly initializes everything due to the method calls in vars above
        System.out.println("Shaders initialized");
    }

    private static int createShaderProgram(String shadername)
    {
        int program = glCreateProgram();
        if (program == 0) throw new RuntimeException("Could not create shader program");


        switch (shadername)
        {
            case "lights":
            {
                glAttachShader(program, LIGHTS_VERT);
                glAttachShader(program, LIGHTS_FRAG);

                glBindAttribLocation(program, LIGHTS_ATTRIB_POSITION, "attrib_position");
                glBindAttribLocation(program, LIGHTS_ATTRIB_TEXTURE_COORDS, "attrib_texture_coords");
                glBindAttribLocation(program, LIGHTS_ATTRIB_FADE, "attrib_fade");
                glBindAttribLocation(program, LIGHTS_ATTRIB_COLOR, "attrib_color");
                glBindAttribLocation(program, LIGHTS_ATTRIB_NORMALS, "attrib_normals");

                glBindFragDataLocation(program, LIGHTS_FRAGDATA_COLOR, "fragdata_color");

                break;
            }
            default:
            {
                glDeleteProgram(program);
                throw new IllegalArgumentException("Shader name not recognized: " + shadername);
            }
        }

        glLinkProgram(program);
        glValidateProgram(program);

        return program;
    }

    private static int createShader(String name)
    {
        int shaderID;
        switch (name.substring(name.length() - 4))
        {
            case "vert":
            {
                shaderID = glCreateShader(GL_VERTEX_SHADER);
                break;
            }
            case "frag":
            {
                shaderID = glCreateShader(GL_FRAGMENT_SHADER);
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Shader type not yet supported: " + name);
            }
        }

        try
        {
            StringBuilder s = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(MODID, name)).getInputStream()));
            String line = reader.readLine();
            while (line != null)
            {
                s.append(line).append("\r\n");
                line = reader.readLine();
            }
            glShaderSource(shaderID, s.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        glCompileShader(shaderID);
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE)
        {
            throw new RuntimeException("Shader did not compile: " + name + "\r\n" + glGetShaderInfoLog(shaderID, 1000));
        }

        return shaderID;
    }
}
