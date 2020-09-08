package com.fantasticsource.luminous;

import com.fantasticsource.fantasticlib.api.FLibAPI;
import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.Set;

import static com.fantasticsource.luminous.Luminous.MODID;

public class LightDataHandler
{
    public static int setModdedLight(WorldServer world, BlockPos pos, String modid, String id, int light)
    {
        world.profiler.startSection(Luminous.NAME + ": setModdedLight");

        String fullID = modid + ":" + id;

        NBTTagCompound compound = FLibAPI.getNBTCap(world).getCompound(MODID);
        if (light == 0)
        {
            NBTTagCompound subCompound = MCTools.getSubCompoundIfExists(compound, "array", "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());
            if (subCompound != null && subCompound.hasKey(fullID))
            {
                int oldVal = subCompound.getInteger(fullID);

                subCompound.removeTag(fullID);

                int max = 0;
                Set<String> keySet = subCompound.getKeySet();
                for (String key : keySet)
                {
                    int light2 = subCompound.getInteger(key);
                    if (light2 > max) max = light2;
                }
                setCurrentModdedLight(world, pos, max);

                if (keySet.size() == 0) MCTools.removeSubNBTAndClean(compound, "array", "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());

                world.profiler.endSection();
                return oldVal;
            }

            world.profiler.endSection();
            return 0;
        }
        else
        {
            NBTTagCompound subCompound = MCTools.getOrGenerateSubCompound(compound, "array", "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());

            int oldVal = subCompound.getInteger(fullID);

            subCompound.setInteger(fullID, light);

            int max = light;
            for (String key : subCompound.getKeySet())
            {
                int light2 = subCompound.getInteger(key);
                if (light2 > max) max = light2;
            }
            setCurrentModdedLight(world, pos, max);

            world.profiler.endSection();
            return oldVal;
        }
    }


    public static int setCurrentClientModdedLight(World world, Network.UpdateModdedLightPacket packet)
    {
        return setCurrentModdedLight(world, packet.pos, packet.value);
    }

    private static int setCurrentModdedLight(World world, BlockPos pos, int light)
    {
        world.profiler.startSection(Luminous.NAME + ": setCurrentModdedLight");

        Chunk chunk = world.getChunkFromBlockCoords(pos);

        //Remove
        if (light == 0)
        {
            Integer oldVal = chunk.moddedBlockLights.remove(pos);
            if (oldVal != null) updateModdedLight(world, chunk, pos, 0);

            world.profiler.endSection();
            return oldVal == null ? 0 : oldVal;
        }

        //Set / change
        Integer oldVal = chunk.moddedBlockLights.put(pos, light);
        if (oldVal == null || oldVal != light) updateModdedLight(world, chunk, pos, light);

        world.profiler.endSection();
        return oldVal == null ? 0 : oldVal;
    }


    protected static void updateModdedLight(World world, Chunk chunk, BlockPos pos, int light)
    {
        world.profiler.startSection(Luminous.NAME + ": updateModdedLight");

        //Force local light update
        for (BlockPos involved : new BlockPos[]{pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.west(), pos.east()})
        {
            world.checkLightFor(EnumSkyBlock.BLOCK, involved);
        }


        //If server...
        if (world instanceof WorldServer)
        {
            //Save persistent modded light data
            updateModdedLightNBTCap((WorldServer) world, chunk, pos, light);

            //Sync to client
            PlayerChunkMapEntry playerChunkMapEntry = ((WorldServer) world).getPlayerChunkMap().getEntry(chunk.x, chunk.z);
            if (playerChunkMapEntry != null)
            {
                for (EntityPlayerMP player : playerChunkMapEntry.getWatchingPlayers())
                {
                    Network.WRAPPER.sendTo(new Network.UpdateModdedLightPacket(pos, light), player);
                }
            }
        }

        world.profiler.endSection();
    }


    protected static void updateModdedLightNBTCap(WorldServer world, Chunk chunk, BlockPos pos, int light)
    {
        world.profiler.startSection(Luminous.NAME + ": updateModdedLightNBTCap");

        NBTTagCompound compound = FLibAPI.getNBTCap(world).getCompound(MODID);
        if (light == 0) MCTools.removeSubNBTAndClean(compound, "current", "" + chunk.x, "" + chunk.z, "" + pos.getY(), "" + pos.getX(), "" + pos.getZ());
        else
        {
            compound = MCTools.getOrGenerateSubCompound(compound, "current", "" + chunk.x, "" + chunk.z, "" + pos.getY(), "" + pos.getX());
            compound.setInteger("" + pos.getZ(), light);
        }

        world.profiler.endSection();
    }


    @SubscribeEvent
    public static void loadChunk(ChunkEvent.Load event)
    {
        World world = event.getWorld();
        if (!(world instanceof WorldServer)) return;

        Chunk chunk = event.getChunk();
        NBTTagCompound compound = MCTools.getSubCompoundIfExists(FLibAPI.getNBTCap(world).getCompound(MODID), "current", "" + chunk.x, "" + chunk.z);
        if (compound == null) return;


        world.profiler.startSection(Luminous.NAME + ": loadChunk");

        for (String yString : compound.getKeySet())
        {
            int y = Integer.parseInt(yString);
            NBTTagCompound yCompound = compound.getCompoundTag(yString);

            for (String xString : yCompound.getKeySet())
            {
                int x = Integer.parseInt(xString);
                NBTTagCompound xCompound = yCompound.getCompoundTag(xString);

                for (String zString : xCompound.getKeySet())
                {
                    int z = Integer.parseInt(zString);
                    int light = xCompound.getInteger(zString);
                    setCurrentModdedLight(world, new BlockPos(x, y, z), light);
                }
            }
        }

        world.profiler.endSection();
    }

    @SubscribeEvent
    public static void watchChunk(ChunkWatchEvent event)
    {
        EntityPlayerMP player = event.getPlayer();
        World world = player.world;

        world.profiler.startSection(Luminous.NAME + ": loadChunk");

        Chunk chunk = event.getChunkInstance();

        //TODO batch these
        for (Map.Entry<BlockPos, Integer> entry : chunk.moddedBlockLights.entrySet())
        {
            Network.WRAPPER.sendTo(new Network.UpdateModdedLightPacket(entry.getKey(), entry.getValue()), player);
        }

        world.profiler.endSection();
    }
}
