package com.fantasticsource.luminous;

import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.lang.reflect.Method;

public class Test1
{
    public static Method worldGetRawLightMethod = ReflectionTool.getMethod(World.class, new String[]{"func_175638_a", "getRawLight"});

    public static int worldGetRawLightRedirect(World world, BlockPos pos, EnumSkyBlock lightType)
    {
        //For some reason, returning a 0 or 15 here lags the crap out of the game (possibly only when generating chunks?)
        //I also think this might be more specific to sky light than block light
//        if (pos.getX() % 16 == 0 && pos.getZ() % 16 == 0) return 15;
        return (int) ReflectionTool.invoke(worldGetRawLightMethod, world, pos, lightType);
    }
}
