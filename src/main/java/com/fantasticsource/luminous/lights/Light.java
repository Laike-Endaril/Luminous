package com.fantasticsource.luminous.lights;

import com.fantasticsource.luminous.Luminous;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Color;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;

public abstract class Light
{
    public static double maxCheckDistance = 0;

    public World world;
    public Vec3d position;
    public Color color;
    protected double fadeStartDistance, fadeStartDistanceSquared, fadeEndDistance, fadeEndDistanceSquared;

    public Light(World world, Vec3d position, Color color, double fadeStartDistance, double fadeEndDistance)
    {
        this.world = world;
        this.position = position;
        this.color = color;
        setFadeStartDistance(fadeStartDistance);
        setFadeEndDistance(fadeEndDistance);

        Luminous.LIGHTS.computeIfAbsent(world, o -> new ArrayList<>()).add(this);
    }


    public void setFadeStartDistance(double fadeStartDistance)
    {
        this.fadeStartDistance = fadeStartDistance;
        fadeStartDistanceSquared = fadeStartDistance * fadeStartDistance;
    }

    public void setFadeEndDistance(double fadeEndDistance)
    {
        this.fadeEndDistance = fadeEndDistance;
        fadeEndDistanceSquared = fadeEndDistance * fadeEndDistance;
        if (fadeEndDistance > maxCheckDistance) maxCheckDistance = fadeEndDistance;
    }


    public double getFadeStartDistance()
    {
        return fadeStartDistance;
    }

    public double getFadeStartDistanceSquared()
    {
        return fadeStartDistanceSquared;
    }

    public double getFadeEndDistance()
    {
        return fadeEndDistance;
    }

    public double getFadeEndDistanceSquared()
    {
        return fadeEndDistanceSquared;
    }


    public abstract Color getLightColorAt(Vec3d vec);


    public int getMCLightIntensityAt(Vec3d vec)
    {
        Color c = getLightColorAt(vec);
        return Tools.max(c.r(), c.g(), c.b()) / 16;
    }
}
