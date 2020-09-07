package com.fantasticsource.luminous.lights.lifeline;

import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class LightActivatorEntityActive extends LightActivatorEntity
{
    protected static final Predicate<Entity> PREDICATE = entity -> entity.isAddedToWorld() || entity.isDead;

    public LightActivatorEntityActive(Entity entity)
    {
        super(entity, PREDICATE);
    }
}
