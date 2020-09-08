package com.fantasticsource.luminous.lights.type;

import com.fantasticsource.luminous.lights.intensity.LightIntensity;
import com.fantasticsource.luminous.lights.lifeline.LightActivator;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;

public class LightEntity extends Light
{
    public final Entity entity;

    public LightEntity(String modid, String id, LightActivator lightActivator, LightIntensity lightIntensity, Entity entity)
    {
        super(modid, id, lightActivator, lightIntensity);

        this.entity = entity;
        this.world = (WorldServer) entity.world;
        this.pos = entity.getPosition();
    }

    @Override
    public void reposition()
    {
        world = (WorldServer) entity.world;
        pos = entity.getPosition();
    }
}
