package com.fantasticsource.luminous;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.fantasticsource.luminous.Luminous.MODID;

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
        public EnumSkyBlock type;
        public Integer value;

        public UpdateModdedLightPacket()
        {
            //Required
        }

        public UpdateModdedLightPacket(BlockPos pos, EnumSkyBlock type, Integer value)
        {
            this.pos = pos;
            this.type = type;
            this.value = value;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
            buf.writeBoolean(type == EnumSkyBlock.BLOCK);
            buf.writeBoolean(value != null);
            if (value != null) buf.writeInt(value);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            type = buf.readBoolean() ? EnumSkyBlock.BLOCK : EnumSkyBlock.SKY;
            value = buf.readBoolean() ? buf.readInt() : null;
        }
    }

    public static class UpdateModdedLightPacketHandler implements IMessageHandler<UpdateModdedLightPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(UpdateModdedLightPacket packet, MessageContext ctx)
        {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> LightHandler.setCurrentClientModdedLight(mc.world, packet));
            return null;
        }
    }
}
