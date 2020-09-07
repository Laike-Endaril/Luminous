package com.fantasticsource.luminous.lights;

import com.fantasticsource.luminous.Luminous;
import com.fantasticsource.luminous.lights.intensity.LightIntensityStatic;
import com.fantasticsource.luminous.lights.lifeline.LightActivatorEntityActive;
import com.fantasticsource.luminous.lights.type.Light;
import com.fantasticsource.luminous.lights.type.LightEntity;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedHashMap;

import static com.fantasticsource.luminous.Luminous.MODID;

public class LightHandler
{
    protected static final LinkedHashMap<String, Light> LIGHTS = new LinkedHashMap<>();

    public static boolean addMovingLightToEntity(String modid, Entity entity, int light)
    {
        String fullID = modid + ":" + entity.getPersistentID();
        if (!LIGHTS.containsKey(fullID))
        {
            LIGHTS.put(fullID, new LightEntity(MODID, "" + entity.getPersistentID(), EnumSkyBlock.BLOCK, new LightActivatorEntityActive(entity), new LightIntensityStatic(light), entity));
            return true;
        }
        return false;
    }

    public static boolean removeMovingLightFromEntity(String modid, Entity entity)
    {
        Light light = LIGHTS.remove(modid + ":" + entity.getPersistentID());
        if (light != null)
        {
            light.clean();
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void tickLights(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;

        Profiler profiler = FMLCommonHandler.instance().getMinecraftServerInstance().profiler;
        profiler.startSection(Luminous.NAME + ": tickLights");
        for (Light light : LIGHTS.values()) light.tick();
        profiler.endSection();
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload event)
    {
        World world = event.getWorld();
        if (world.isRemote) return;

        LIGHTS.entrySet().removeIf(entry -> entry.getValue().world == world);
    }
}
