package com.fantasticsource.luminous;

import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;

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
        Network.init();
        MinecraftForge.EVENT_BUS.register(Luminous.class);
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }


    public static boolean setLightOverride(World world, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        LinkedHashMap<BlockPos, Integer> map = type == EnumSkyBlock.SKY ? chunk.skyLightOverrides : chunk.blockLightOverrides;

        //Remove
        if (light == null)
        {
            if (map.remove(pos) != null)
            {
                updateLight(world, chunk, pos, type, null);
                return true;
            }
            return false;
        }

        //Set / change
        Integer oldVal = map.put(pos, light);
        if (oldVal == null || oldVal != light.intValue())
        {
            updateLight(world, chunk, pos, type, light);
            return true;
        }
        return false;
    }


    protected static void updateLight(World world, Chunk centerChunk, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        //Force local light update
        for (BlockPos involved : new BlockPos[]{pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.west(), pos.east()})
        {
            world.checkLightFor(type, involved);
        }


        //If server, forward light update packet to tracking clients
        if (world instanceof WorldServer)
        {
            PlayerChunkMapEntry playerChunkMapEntry = ((WorldServer) world).getPlayerChunkMap().getEntry(centerChunk.x, centerChunk.z);
            if (playerChunkMapEntry != null)
            {
                for (EntityPlayerMP player : playerChunkMapEntry.getWatchingPlayers())
                {
                    Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(pos, type, light), player);
                }
            }
        }
    }


    @SubscribeEvent
    public static void test(EntityJoinWorldEvent event)
    {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !(entity instanceof EntitySnowball)) return;

        WorldServer world = (WorldServer) entity.world;
        BlockPos pos = entity.getPosition().down();
        if (!setLightOverride(world, pos, EnumSkyBlock.BLOCK, 15)) setLightOverride(world, pos, EnumSkyBlock.BLOCK, null);
    }
}
