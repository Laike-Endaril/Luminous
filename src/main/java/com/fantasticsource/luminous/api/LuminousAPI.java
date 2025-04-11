package com.fantasticsource.luminous.api;

import com.fantasticsource.luminous.Luminous;
import com.fantasticsource.luminous.lights.Light;
import com.fantasticsource.luminous.lights.LightSphere;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;

public class LuminousAPI
{
    public static LightSphere addStaticSphereLight(World world, Vec3d position, Color color, double fadeStartDistance, double fadeEndDistance)
    {
        return new LightSphere(world, position, color, fadeStartDistance, fadeEndDistance);
    }

    public static void removeLight(Light light)
    {
        ArrayList<Light> lights = Luminous.LIGHTS.get(light.world);
        if (lights == null) return;

        if (lights.remove(light) && lights.size() == 0) Luminous.LIGHTS.remove(light.world);
    }


    public static ArrayList<Light> getLights(World world)
    {
        return Luminous.LIGHTS.getOrDefault(world, new ArrayList<>());
    }

    public static Color getSummedColorAt(World world, Vec3d vec)
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

    public static Color getMaxColorAt(World world, Vec3d vec)
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


    public static int getBasicVanillaLightLevelAt(World world, Vec3d vec, EnumSkyBlock lightType)
    {
        int light = world.getLightFor(lightType, new BlockPos(vec));
        return lightType == EnumSkyBlock.BLOCK ? light : Math.max(0, light - world.getSkylightSubtracted());
    }


    public static int getSummedVanillaLightLevelAt(World world, Vec3d vec)
    {
        return Tools.min(15, getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.BLOCK) + getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.SKY));
    }

    public static int getMaxVanillaLightLevelAt(World world, Vec3d vec)
    {
        return Tools.max(getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.BLOCK), getBasicVanillaLightLevelAt(world, vec, EnumSkyBlock.SKY));
    }

    public static int getSummedModLightLevelAt(World world, Vec3d vec)
    {
        Color c = getSummedColorAt(world, vec);
        return Tools.max(c.r(), c.g(), c.b()) / 16;
    }

    public static int getMaxModLightLevelAt(World world, Vec3d vec)
    {
        Color c = getMaxColorAt(world, vec);
        return Tools.max(c.r(), c.g(), c.b()) / 16;
    }


    public static int getSummedFinalLightLevelAt(World world, Vec3d vec)
    {
        return Tools.min(15, getSummedVanillaLightLevelAt(world, vec) + getSummedModLightLevelAt(world, vec));
    }

    public static int getMaxFinalLightLevelAt(World world, Vec3d vec)
    {
        return Tools.max(getMaxVanillaLightLevelAt(world, vec), getMaxModLightLevelAt(world, vec));
    }
}
