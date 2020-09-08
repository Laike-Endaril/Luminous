package com.fantasticsource.luminous.lights.type;

import com.fantasticsource.luminous.LightDataHandler;
import com.fantasticsource.luminous.lights.intensity.LightIntensity;
import com.fantasticsource.luminous.lights.lifeline.LightActivator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public abstract class Light
{
    public final String modid, id;

    public final LightActivator lightActivator;
    public final LightIntensity lightIntensity;

    public WorldServer world = null;
    public BlockPos pos = null;

    protected WorldServer prevWorld = null;
    protected BlockPos prevPos = null;
    protected int prevIntensity = 0;


    public Light(String modid, String id, LightActivator lightActivator, LightIntensity lightIntensity)
    {
        this.modid = modid;
        this.id = id;

        this.lightActivator = lightActivator;
        this.lightIntensity = lightIntensity;
    }

    public final void tick()
    {
        if (!lightActivator.active())
        {
            clean();
            return;
        }

        prevWorld = world;
        prevPos = pos;

        reposition();

        lightIntensity.tick();


        boolean moved = world != prevWorld || pos != prevPos;
        int intensity = lightIntensity.intensity();
        if (moved || intensity != prevIntensity) LightDataHandler.setModdedLight(world, pos, modid, id, intensity);
        if (moved) LightDataHandler.setModdedLight(prevWorld, prevPos, modid, id, 0);
    }


    public final void clean()
    {
        LightDataHandler.setModdedLight(world, pos, modid, id, 0);
    }


    public void reposition()
    {
    }
}
