package com.fantasticsource.luminous;

import com.fantasticsource.mctools.MCTools;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Luminous.MODID, name = Luminous.NAME, version = Luminous.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.036,)")
public class Luminous
{
    public static final String MODID = "luminous";
    public static final String NAME = "Luminous";
    public static final String VERSION = "1.12.2.000";

    static
    {
        if (!MCTools.devEnv())
        {
            LaunchClassLoader classLoader = (LaunchClassLoader) Luminous.class.getClassLoader();
            classLoader.registerTransformer("com.fantasticsource.luminous.LuminousTransformer");
        }
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(Luminous.class);
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    public static void setBlockLightOverride(WorldServer world, BlockPos pos, int light)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        Integer oldVal = chunk.blockLightOverrides.put(pos, light);
        if (oldVal == null || oldVal != light) world.getPlayerChunkMap().markBlockForUpdate(pos);
    }

    public static void removeBlockLightOverride(WorldServer world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (chunk.blockLightOverrides.remove(pos) != null) world.getPlayerChunkMap().markBlockForUpdate(pos);
    }

    public static void setSkyLightOverride(WorldServer world, BlockPos pos, int light)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        Integer oldVal = chunk.skyLightOverrides.put(pos, light);
        if (oldVal == null || oldVal != light) world.getPlayerChunkMap().markBlockForUpdate(pos);
    }

    public static void removeSkyLightOverride(WorldServer world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (chunk.skyLightOverrides.remove(pos) != null) world.getPlayerChunkMap().markBlockForUpdate(pos);
    }
}
