package com.fantasticsource.luminous;

import com.fantasticsource.luminous.lights.Light;
import com.fantasticsource.luminous.shaders.Shaders;
import com.fantasticsource.luminous.shaders.VboRenderListEdit;
import com.fantasticsource.tools.ReflectionTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.fantasticsource.luminous.Luminous.*;

@Mod(modid = MODID, name = NAME, version = VERSION, dependencies = "required-after:fantasticlib@[1.12.2.051,)")
public class Luminous
{
    public static final String MODID = "luminous";
    public static final String NAME = "Luminous";
    public static final String VERSION = "1.12.2.000";

    public static final LinkedHashMap<WorldServer, ArrayList<Light>> SERVER_LIGHTS = new LinkedHashMap<>();
    public static final ArrayList<Light> CLIENT_LIGHTS = new ArrayList<>();

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(Luminous.class);
        Network.init();
    }

    @SideOnly(Side.CLIENT)
    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event)
    {
        //TODO add variant for non-vbo mode
        Shaders.init();
        ReflectionTool.set(RenderGlobal.class, new String[]{"field_174996_N", "renderContainer"}, Minecraft.getMinecraft().renderGlobal, new VboRenderListEdit());
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    @SubscribeEvent
    public static void entityJoinWorld(EntityJoinWorldEvent event)
    {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayerMP) Network.WRAPPER.sendTo(new Network.LightsPacket((WorldServer) event.getWorld()), (EntityPlayerMP) entity);
    }
}
