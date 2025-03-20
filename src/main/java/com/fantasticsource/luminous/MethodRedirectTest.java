package com.fantasticsource.luminous;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

public class MethodRedirectTest
{
    public static int worldGetRawLightRedirect(BlockPos pos, EnumSkyBlock lightType)
    {
        return 0;
    }
}
