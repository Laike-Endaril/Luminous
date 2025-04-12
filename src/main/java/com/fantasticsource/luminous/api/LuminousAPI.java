package com.fantasticsource.luminous.api;

import com.fantasticsource.luminous.Luminous;
import com.fantasticsource.luminous.lights.Light;
import com.fantasticsource.luminous.lights.LightSphere;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;

public class LuminousAPI
{
    public static LightSphere addStaticSphereLight(WorldServer world, Vec3d position, Color color, double fadeStartDistance, double fadeEndDistance)
    {
        LightSphere light = new LightSphere(position, color, fadeStartDistance, fadeEndDistance);
        light.setWorldInternal(world);
        return light;
    }

    public static void removeLight(Light light)
    {
        if (light.getWorld() instanceof WorldServer) light.setWorldInternal(null);
    }


    public static ArrayList<Light> getLights(WorldServer world)
    {
        return Luminous.SERVER_LIGHTS.getOrDefault(world, new ArrayList<>());
    }

    public static Color getSummedColorAt(WorldServer world, Vec3d vec)
    {
        int r = 0, g = 0, b = 0;
        Color c;
        for (Light light : getLights(world))
        {
            c = light.getLightColorAt(vec);
            r += c.r();
            g += c.g();
            b += c.b();
        }
        return new Color(r, g, b);
    }

    public static Color getMaxColorAt(WorldServer world, Vec3d vec)
    {
        int r = 0, g = 0, b = 0;
        Color c;
        for (Light light : getLights(world))
        {
            c = light.getLightColorAt(vec);
            r = Tools.max(r, c.r());
            g = Tools.max(g, c.g());
            b = Tools.max(b, c.b());
        }
        return new Color(r, g, b);
    }


    public static int getBasicVanillaLightLevelAt(WorldServer world, Vec3d vec, EnumSkyBlock lightType)
    {
        int light = world.getLightFor(lightType, new BlockPos(vec));
        return lightType == EnumSkyBlock.BLOCK ? light : Math.max(0, light - world.getSkylightSubtracted());
    }


    public static int getSummedVanillaLightLevelAt(WorldServer world, Vec3d vec)
    {
        return Tools.min(15, getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.BLOCK) + getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.SKY));
    }

    public static int getMaxVanillaLightLevelAt(WorldServer world, Vec3d vec)
    {
        return Tools.max(getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.BLOCK), getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.SKY));
    }

    public static int getSummedModLightLevelAt(WorldServer world, Vec3d vec)
    {
        Color c = getSummedColorAt(world, vec);
        return Tools.max(c.r(), c.g(), c.b()) / 16;
    }

    public static int getMaxModLightLevelAt(WorldServer world, Vec3d vec)
    {
        Color c = getMaxColorAt(world, vec);
        return Tools.max(c.r(), c.g(), c.b()) / 16;
    }


    public static int getSummedFinalLightLevelAt(WorldServer world, Vec3d vec)
    {
        return Tools.min(15, getSummedVanillaLightLevelAt(world, vec) + getSummedModLightLevelAt(world, vec));
    }

    public static int getMaxFinalLightLevelAt(WorldServer world, Vec3d vec)
    {
        return Tools.max(getMaxVanillaLightLevelAt(world, vec), getMaxModLightLevelAt(world, vec));
    }
}
