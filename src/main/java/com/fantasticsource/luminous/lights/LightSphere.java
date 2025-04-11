package com.fantasticsource.luminous.lights;

import com.fantasticsource.mctools.ImprovedRayTracing;
import com.fantasticsource.tools.datastructures.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LightSphere extends Light
{
    public LightSphere(World world, Vec3d position, Color color, double fadeStartDistance, double fadeEndDistance)
    {
        super(world, position, color, fadeStartDistance, fadeEndDistance);
    }

    @Override
    public Color getLightColorAt(Vec3d vec)
    {
        if (!world.isAreaLoaded(new BlockPos(vec), new BlockPos(position))) return new Color(0);


        double distSquared = position.squareDistanceTo(vec);
        if (distSquared > fadeEndDistanceSquared) return new Color(0);

        if (!ImprovedRayTracing.isUnobstructed(world, position, vec, false)) return new Color(0);

        if (distSquared <= fadeStartDistanceSquared) return color.copy();

        double dist = Math.sqrt(distSquared);
        float ratio = 1f - (float) ((dist - fadeStartDistance) / (fadeEndDistance - fadeStartDistance));
        return new Color(color.rf() * ratio, color.gf() * ratio, color.bf() * ratio);
    }
}
