package com.fantasticsource.luminous.lights.type;

import com.fantasticsource.luminous.lights.intensity.LightIntensity;
import com.fantasticsource.luminous.lights.lifeline.LightActivator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;

public class LightStatic extends Light
{
    public LightStatic(String modid, String id, EnumSkyBlock type, LightActivator lightActivator, LightIntensity lightIntensity, WorldServer world, BlockPos pos)
    {
        super(modid, id, type, lightActivator, lightIntensity);

        this.world = world;
        this.pos = pos;
    }
}
