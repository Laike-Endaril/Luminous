package com.fantasticsource.luminous.lights.lifeline;

import java.util.function.Predicate;

public abstract class LightActivator<T>
{
    public final Predicate<T> predicate;

    public LightActivator(Predicate<T> predicate)
    {
        this.predicate = predicate;
    }

    public abstract boolean active();
}
