package com.fantasticsource.luminous;

import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
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


    public static boolean setBlockLightOverride(WorldServer world, BlockPos pos, int light)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        Integer oldVal = chunk.blockLightOverrides.put(pos, light);
        if (oldVal == null || oldVal != light)
        {
            updateLight(world, chunk, pos, EnumSkyBlock.BLOCK);
            return true;
        }
        return false;
    }

    public static boolean removeBlockLightOverride(WorldServer world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (chunk.blockLightOverrides.remove(pos) != null)
        {
            updateLight(world, chunk, pos, EnumSkyBlock.BLOCK);
            return true;
        }
        return false;
    }

    public static boolean setSkyLightOverride(WorldServer world, BlockPos pos, int light)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        Integer oldVal = chunk.skyLightOverrides.put(pos, light);
        if (oldVal == null || oldVal != light)
        {
            updateLight(world, chunk, pos, EnumSkyBlock.BLOCK);
            return true;
        }
        return false;
    }

    public static boolean removeSkyLightOverride(WorldServer world, BlockPos pos)
    {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (chunk.skyLightOverrides.remove(pos) != null)
        {
            updateLight(world, chunk, pos, EnumSkyBlock.BLOCK);
            return true;
        }
        return false;
    }


    protected static void updateLight(WorldServer world, Chunk centerChunk, BlockPos pos, EnumSkyBlock type)
    {
        //Force light update on server
        for (BlockPos involved : new BlockPos[]{pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.west(), pos.east()}) world.checkLightFor(type, involved);


        //Force light update on client (from server)
        int yLayer = pos.getY() >> 4;
        int changedLayerFlags = 1 << yLayer;
        if (yLayer > 0) changedLayerFlags |= 1 << (yLayer - 1);
        if (yLayer < 15) changedLayerFlags |= 1 << (yLayer + 1);

        for (int xOffset = -1; xOffset <= 1; xOffset++)
        {
            for (int zOffset = -1; zOffset <= 1; zOffset++)
            {
                PlayerChunkMapEntry playerChunkMapEntry = world.getPlayerChunkMap().getEntry(centerChunk.x + xOffset, centerChunk.z + zOffset);
                if (playerChunkMapEntry != null)
                {
                    Packet<?> packet = new SPacketChunkData(centerChunk, changedLayerFlags);
                    for (EntityPlayerMP player : playerChunkMapEntry.getWatchingPlayers()) player.connection.sendPacket(packet);
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
        if (!setBlockLightOverride(world, pos, 15)) removeBlockLightOverride(world, pos);
    }
}
