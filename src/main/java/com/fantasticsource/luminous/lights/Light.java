package com.fantasticsource.luminous.lights;

import com.fantasticsource.luminous.Luminous;
import com.fantasticsource.luminous.Network;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Color;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;

public abstract class Light
{
    public static double maxCheckDistance = 0;

    protected World world = null;
    protected Vec3d position;
    protected Color color;
    protected double fadeStartDistance, fadeStartDistanceSquared, fadeEndDistance, fadeEndDistanceSquared;

    public Light(Vec3d position, Color color, double fadeStartDistance, double fadeEndDistance)
    {
        this.position = position;
        this.color = color;

        this.fadeStartDistance = fadeStartDistance;
        fadeStartDistanceSquared = fadeStartDistance * fadeStartDistance;

        this.fadeEndDistance = fadeEndDistance;
        fadeEndDistanceSquared = fadeEndDistance * fadeEndDistance;
        if (fadeEndDistance > maxCheckDistance) maxCheckDistance = fadeEndDistance;
    }


    public void setWorldInternal(World world)
    {
        if (this.world == world) return;


        if (this.world instanceof WorldServer)
        {
            ArrayList<Light> lights = Luminous.SERVER_LIGHTS.get(this.world);
            if (lights != null)
            {
                int index = lights.indexOf(this);
                if (index != -1)
                {
                    lights.remove(index);
                    if (lights.size() == 0) Luminous.SERVER_LIGHTS.remove(this.world);
                    for (EntityPlayer player : this.world.playerEntities) Network.WRAPPER.sendTo(new Network.RemoveLightPacket(index), (EntityPlayerMP) player);
                }
            }
        }


        if (world == null) return;

        if (world instanceof WorldServer)
        {
            this.world = world;
            if (world != null)
            {
                Luminous.SERVER_LIGHTS.computeIfAbsent((WorldServer) world, o -> new ArrayList<>()).add(this);
                for (EntityPlayer player : world.playerEntities) Network.WRAPPER.sendTo(new Network.LightsPacket(this), (EntityPlayerMP) player);
            }
        }
        else
        {
            this.world = world;
            Luminous.CLIENT_LIGHTS.add(this);
        }
    }


    public World getWorld()
    {
        return world;
    }

    public Vec3d getPosition()
    {
        return position;
    }

    public Color getColor()
    {
        return color.copy();
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
