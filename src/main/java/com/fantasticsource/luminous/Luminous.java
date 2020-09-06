package com.fantasticsource.luminous;

import com.fantasticsource.fantasticlib.api.FLibAPI;
import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod(modid = Luminous.MODID, name = Luminous.NAME, version = Luminous.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.036x,)")
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
        FLibAPI.attachNBTCapToWorldIf(MODID, o -> o instanceof WorldServer);
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


    protected static void updateLight(World world, Chunk chunk, BlockPos pos, EnumSkyBlock type, Integer light)
    {
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
    }


    protected static void updateLightOverrideNBTCap(WorldServer world, Chunk chunk, BlockPos pos, EnumSkyBlock type, Integer light)
    {
        NBTTagCompound compound = FLibAPI.getNBTCap(world).getCompound(MODID);
        if (light == null) MCTools.removeSubNBTAndClean(compound, "" + chunk.x, "" + chunk.z, type.name(), "" + pos.getY(), "" + pos.getX(), "" + pos.getZ());
        else
        {
            compound = MCTools.getOrGenerateSubCompound(compound, "" + chunk.x, "" + chunk.z, type.name(), "" + pos.getY(), "" + pos.getX());
            compound.setInteger("" + pos.getZ(), light);
        }
    }

    @SubscribeEvent
    public static void loadChunk(ChunkEvent.Load event)
    {
        World world = event.getWorld();
        if (!(world instanceof WorldServer)) return;

        Chunk chunk = event.getChunk();
        NBTTagCompound compound = MCTools.getSubCompoundIfExists(FLibAPI.getNBTCap(world).getCompound(MODID), "" + chunk.x, "" + chunk.z);
        if (compound == null) return;


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
                        setLightOverride(world, new BlockPos(x, y, z), type, light);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void watchChunk(ChunkWatchEvent event)
    {
        EntityPlayerMP player = event.getPlayer();
        Chunk chunk = event.getChunkInstance();

        //TODO batch these
        for (Map.Entry<BlockPos, Integer> entry : chunk.skyLightOverrides.entrySet())
        {
            Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(entry.getKey(), EnumSkyBlock.SKY, entry.getValue()), player);
        }

        //TODO batch these
        for (Map.Entry<BlockPos, Integer> entry : chunk.blockLightOverrides.entrySet())
        {
            Network.WRAPPER.sendTo(new Network.UpdateLightOverridePacket(entry.getKey(), EnumSkyBlock.BLOCK, entry.getValue()), player);
        }
    }


//    @SubscribeEvent
//    public static void test(EntityJoinWorldEvent event)
//    {
//        Entity entity = event.getEntity();
//        if (entity.world.isRemote || !(entity instanceof EntitySnowball)) return;
//
//        WorldServer world = (WorldServer) entity.world;
//        BlockPos pos = entity.getPosition().down();
//        if (!setLightOverride(world, pos, EnumSkyBlock.BLOCK, 15))
//        {
//            setLightOverride(world, pos, EnumSkyBlock.BLOCK, null);
//        }
//    }


    protected static World[] litWorlds = new World[]{null, null};
    protected static BlockPos[] litPositions = new BlockPos[]{null, null};

    @SubscribeEvent
    public static void test(TickEvent.PlayerTickEvent event)
    {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) return;


        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.world, recentWorld = litWorlds[0], oldWorld = litWorlds[1];
        BlockPos eyePos = new BlockPos(player.getPositionEyes(0)), recentPos = litPositions[0], oldPos = litPositions[1];

        if (world != recentWorld || !eyePos.equals(recentPos))
        {
            if (world == oldWorld && eyePos.equals(oldPos))
            {
                litWorlds[1] = recentWorld;
                litPositions[1] = recentPos;
            }
            else
            {
                if (oldWorld != null)
                {
                    System.out.println("Remove");
                    setLightOverride(oldWorld, oldPos, EnumSkyBlock.BLOCK, null);
                }

                System.out.println("Add");
                setLightOverride(world, eyePos, EnumSkyBlock.BLOCK, 15);

                litWorlds[1] = litWorlds[0];
                litPositions[1] = litPositions[0];
            }

            litWorlds[0] = world;
            litPositions[0] = eyePos;
        }
    }
}
