package com.fantasticsource.luminous;

import com.fantasticsource.luminous.lights.Light;
import com.fantasticsource.luminous.lights.LightSphere;
import com.fantasticsource.tools.datastructures.Color;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

import static com.fantasticsource.luminous.Luminous.MODID;

public class Network
{
    public static final SimpleNetworkWrapper WRAPPER = new SimpleNetworkWrapper(MODID);
    private static int discriminator = 0;

    public static void init()
    {
        WRAPPER.registerMessage(LightsPacketHandler.class, LightsPacket.class, discriminator++, Side.CLIENT);
        WRAPPER.registerMessage(RemoveLightPacketHandler.class, RemoveLightPacket.class, discriminator++, Side.CLIENT);
    }


    public static class LightsPacket implements IMessage
    {
        ArrayList<Light> lights = new ArrayList<>();
        boolean reset;

        public LightsPacket()
        {
            //Required
        }

        public LightsPacket(WorldServer world)
        {
            this(Luminous.SERVER_LIGHTS.getOrDefault(world, new ArrayList<>()), true);
        }

        public LightsPacket(Light light)
        {
            this.lights.add(light);
        }

        public LightsPacket(ArrayList<Light> lights, boolean reset)
        {
            this.lights.addAll(lights);
            this.reset = reset;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeBoolean(reset);
            buf.writeInt(lights.size());
            for (Light light : lights)
            {
                buf.writeDouble(light.getPosition().x);
                buf.writeDouble(light.getPosition().y);
                buf.writeDouble(light.getPosition().z);
                buf.writeInt(light.getColor().color());
                buf.writeDouble(light.getFadeStartDistance());
                buf.writeDouble(light.getFadeEndDistance());
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            reset = buf.readBoolean();
            for (int i = buf.readInt(); i > 0; i--)
            {
                lights.add(new LightSphere(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()), new Color(buf.readInt()), buf.readDouble(), buf.readDouble()));
            }
        }
    }

    public static class LightsPacketHandler implements IMessageHandler<LightsPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(LightsPacket packet, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                if (packet.reset) Luminous.CLIENT_LIGHTS.clear();
                World world = Minecraft.getMinecraft().world;
                for (Light light : packet.lights) light.setWorldInternal(world);
            });
            return null;
        }
    }


    public static class RemoveLightPacket implements IMessage
    {
        int index;

        public RemoveLightPacket()
        {
            //Required
        }

        public RemoveLightPacket(int index)
        {
            this.index = index;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(index);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            index = buf.readInt();
        }
    }

    public static class RemoveLightPacketHandler implements IMessageHandler<RemoveLightPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(RemoveLightPacket packet, MessageContext ctx)
        {
            Minecraft.getMinecraft().addScheduledTask(() ->
            {
                Luminous.CLIENT_LIGHTS.remove(packet.index);
            });
            return null;
        }
    }
}
