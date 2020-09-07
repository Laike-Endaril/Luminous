package com.fantasticsource.luminous.lights.lifeline;

import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class LightActivatorEntity extends LightActivator<Entity>
{
    public final Entity entity;

    public LightActivatorEntity(Entity entity, Predicate<Entity> predicate)
    {
        super(predicate);
        this.entity = entity;
    }

    @Override
    public boolean active()
    {
        return predicate.test(entity);
    }
}
