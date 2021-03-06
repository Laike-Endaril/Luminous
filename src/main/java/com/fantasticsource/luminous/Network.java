package com.fantasticsource.luminous;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.fantasticsource.luminous.asm.LuminousCore.MODID;

public class Network
{
    public static final SimpleNetworkWrapper WRAPPER = new SimpleNetworkWrapper(MODID);
    private static int discriminator = 0;

    public static void init()
    {
        WRAPPER.registerMessage(UpdateModdedLightPacketHandler.class, UpdateModdedLightPacket.class, discriminator++, Side.CLIENT);
    }


    public static class UpdateModdedLightPacket implements IMessage
    {
        public BlockPos pos;
        public int value;

        public UpdateModdedLightPacket()
        {
            //Required
        }

        public UpdateModdedLightPacket(BlockPos pos, int value)
        {
            this.pos = pos;
            this.value = value;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
            buf.writeInt(value);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            value = buf.readInt();
        }
    }

    public static class UpdateModdedLightPacketHandler implements IMessageHandler<UpdateModdedLightPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(UpdateModdedLightPacket packet, MessageContext ctx)
        {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> LightDataHandler.setCurrentClientModdedLight(mc.world, packet));
            return null;
        }
    }
}
