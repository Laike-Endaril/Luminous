package com.fantasticsource.luminous;

import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.lang.reflect.Method;

public class LightHandler
{
    public static Method worldGetRawLightMethod = ReflectionTool.getMethod(World.class, new String[]{"func_175638_a", "getRawLight"});

    public static int worldGetRawLightRedirect(World world, BlockPos pos, EnumSkyBlock lightType)
    {
        if (pos.getX() < 0) return 0;
        return (int) ReflectionTool.invoke(worldGetRawLightMethod, world, pos, lightType);
    }
}
