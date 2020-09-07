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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.fantasticsource.luminous.Luminous.MODID;

public class LightHandler
{
    public static Integer setLightOverride(WorldServer world, BlockPos pos, EnumSkyBlock type, String modid, String id, Integer light)
    {
        world.profiler.startSection(Luminous.NAME + ": setLightOverride");

        String fullID = modid + id;

        NBTTagCompound compound = FLibAPI.getNBTCap(world).getCompound(MODID);
        if (light == null)
        {
            NBTTagCompound subCompound = MCTools.getSubCompoundIfExists(compound, "array", "" + type, "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());
            if (subCompound != null && subCompound.hasKey(fullID))
            {
                Integer oldVal = subCompound.getInteger(fullID);

                subCompound.removeTag(fullID);

                int max = -1;
                Set<String> keySet = subCompound.getKeySet();
                for (String key : keySet)
                {
                    int light2 = subCompound.getInteger(key);
                    if (light2 > max) max = light2;
                }
                setCurrentLightOverride(world, pos, type, max == -1 ? null : max);

                if (keySet.size() == 0) MCTools.removeSubNBTAndClean(compound, "array", "" + type, "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());

                world.profiler.endSection();
                return oldVal;
            }

            world.profiler.endSection();
            return null;
        }
        else
        {
            NBTTagCompound subCompound = MCTools.getOrGenerateSubCompound(compound, "array", "" + type, "" + pos.getX(), "" + pos.getZ(), "" + pos.getY());

            Integer oldVal = subCompound.getInteger(fullID);

            subCompound.setInteger(fullID, light);

            int max = light;
            for (String key : subCompound.getKeySet())
            {
                int light2 = subCompound.getInteger(key);
                if (light2 > max) max = light2;
            }
            setCurrentLightOverride(world, pos, type, max == -1 ? null : max);

            world.profiler.endSection();
            return oldVal;
        }
    }


    public static Integer setCurrentClientLightOverride(World world, Network.UpdateLightOverridePacket packet)
    {
        return setCurrentLightOverride(world, packet.pos, packet.type, packet.value);
    }

    private static Integer setCurrentLightOverride(World world, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        world.profiler.startSection(Luminous.NAME + ": setCurrentLightOverride");

        Chunk chunk = world.getChunkFromBlockCoords(pos);
        LinkedHashMap<BlockPos, Integer> map = type == EnumSkyBlock.SKY ? chunk.moddedSkyLights : chunk.moddedBlockLights;

        //Remove
        if (light == null)
        {
            Integer oldVal = map.remove(pos);
            if (oldVal != null) updateLight(world, chunk, pos, type, null);

            world.profiler.endSection();
            return oldVal;
        }

        //Set / change
        Integer oldVal = map.put(pos, light);
        if (oldVal == null || oldVal != light.intValue()) updateLight(world, chunk, pos, type, light);

        world.profiler.endSection();
        return oldVal;
    }


    protected static void updateLight(World world, Chunk chunk, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        world.profiler.startSection(Luminous.NAME + ": updateLight");

        //Force local light update
        for (BlockPos involved : new BlockPos[]{pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.west(), pos.east()})
        {
            world.checkLightFor(type, involved);
        }


        //If server...
        if (world instanceof WorldServer)
        {
            //Save persistent override data
            updateLightOverrideNBTCap((WorldServer) world, chunk, pos, type, light);

            //Sync to client
            PlayerChunkMapEntry playerChunkMapEntry = ((WorldServer) world).getPlayerChunkMap().getEntry(chunk.x, chunk.z);
            if (playerChunkMapEntry != null)
            {
                for (EntityPlayerMP player : playerChunkMapEntry.getWatchingPlayers())
                {
                    Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(pos, type, light), player);
                }
            }
        }

        world.profiler.endSection();
    }


    protected static void updateLightOverrideNBTCap(WorldServer world, Chunk chunk, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        world.profiler.startSection(Luminous.NAME + ": updateLightOverrideNBTCap");

        NBTTagCompound compound = FLibAPI.getNBTCap(world).getCompound(MODID);
        if (light == null) MCTools.removeSubNBTAndClean(compound, "current", "" + chunk.x, "" + chunk.z, type.name(), "" + pos.getY(), "" + pos.getX(), "" + pos.getZ());
        else
        {
            compound = MCTools.getOrGenerateSubCompound(compound, "current", "" + chunk.x, "" + chunk.z, type.name(), "" + pos.getY(), "" + pos.getX());
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

        for (String typeString : compound.getKeySet())
        {
            EnumSkyBlock type = EnumSkyBlock.valueOf(typeString);
            NBTTagCompound typeCompound = compound.getCompoundTag(typeString);

            for (String yString : typeCompound.getKeySet())
            {
                int y = Integer.parseInt(yString);
                NBTTagCompound yCompound = typeCompound.getCompoundTag(yString);

                for (String xString : yCompound.getKeySet())
                {
                    int x = Integer.parseInt(xString);
                    NBTTagCompound xCompound = yCompound.getCompoundTag(xString);

                    for (String zString : xCompound.getKeySet())
                    {
                        int z = Integer.parseInt(zString);
                        int light = xCompound.getInteger(zString);
                        setCurrentLightOverride(world, new BlockPos(x, y, z), type, light);
                    }
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
        for (Map.Entry<BlockPos, Integer> entry : chunk.moddedSkyLights.entrySet())
        {
            Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(entry.getKey(), EnumSkyBlock.SKY, entry.getValue()), player);
        }

        //TODO batch these
        for (Map.Entry<BlockPos, Integer> entry : chunk.moddedBlockLights.entrySet())
        {
            Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(entry.getKey(), EnumSkyBlock.BLOCK, entry.getValue()), player);
        }

        world.profiler.endSection();
    }
}
