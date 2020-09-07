package com.fantasticsource.luminous;

import com.fantasticsource.fantasticlib.api.FLibAPI;
import com.fantasticsource.luminous.lights.LightHandler;
import com.fantasticsource.luminous.lights.type.Light;
import com.fantasticsource.mctools.MCTools;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;

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
            ((LaunchClassLoader) Luminous.class.getClassLoader()).registerTransformer("com.fantasticsource.luminous.LuminousTransformer");
        }
    }

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        Network.init();
        MinecraftForge.EVENT_BUS.register(Luminous.class);
        MinecraftForge.EVENT_BUS.register(Light.class);
        FLibAPI.attachNBTCapToWorldIf(MODID, o -> o instanceof WorldServer);
        MinecraftForge.EVENT_BUS.register(LightDataHandler.class);
        MinecraftForge.EVENT_BUS.register(LightHandler.class);
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }


    @SubscribeEvent
    public static void staticLightTest(EntityJoinWorldEvent event)
    {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !(entity instanceof EntitySnowball)) return;

        WorldServer world = (WorldServer) entity.world;

        world.profiler.startSection(Luminous.NAME + ": staticLightTest");

        BlockPos pos = entity.getPosition().down();
        if (LightDataHandler.setModdedLight(world, pos, EnumSkyBlock.BLOCK, MODID, "snow", 7) != 0)
        {
            LightDataHandler.setModdedLight(world, pos, EnumSkyBlock.BLOCK, MODID, "snow", 0);
        }

        world.profiler.endSection();
    }


    protected static final LinkedHashMap<EntityLivingBase, WorldServer> LIT_WORLDS = new LinkedHashMap<>();
    protected static final LinkedHashMap<EntityLivingBase, BlockPos> LIT_POSITIONS = new LinkedHashMap<>();

    @SubscribeEvent
    public static void movingLightTest(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase livingBase = event.getEntityLiving();
        if (livingBase.world.isRemote) return;


        WorldServer world = (WorldServer) livingBase.world, litWorld = LIT_WORLDS.get(livingBase);

        world.profiler.startSection(Luminous.NAME + ": movingLightTest");

        BlockPos eyePos = new BlockPos(livingBase.getPositionEyes(0)), litPosition = LIT_POSITIONS.get(livingBase);

        if (world != litWorld || !eyePos.equals(litPosition))
        {
            LightDataHandler.setModdedLight(world, eyePos, EnumSkyBlock.BLOCK, MODID, "" + livingBase.getUniqueID(), 15);
            if (litWorld != null) LightDataHandler.setModdedLight(litWorld, litPosition, EnumSkyBlock.BLOCK, MODID, "" + livingBase.getUniqueID(), 0);
            LIT_WORLDS.put(livingBase, world);
            LIT_POSITIONS.put(livingBase, eyePos);
        }

        world.profiler.endSection();
    }


//    @SubscribeEvent
//    public static void lightsTest(PlayerInteractEvent.EntityInteractSpecific event)
//    {
//        EntityPlayer player = event.getEntityPlayer();
//        if (!(player instanceof EntityPlayerMP) || event.getHand() != EnumHand.MAIN_HAND) return;
//
//        if (!LightHandler.addMovingLightToEntity(MODID, player, 15))
//        {
//            LightHandler.removeMovingLightFromEntity(MODID, player);
//        }
//    }
}
