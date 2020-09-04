package com.fantasticsource.luminous;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
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
        WRAPPER.registerMessage(UpdateLightPacketHandler.class, UpdateLightPacket.class, discriminator++, Side.CLIENT);
    }


    public static class UpdateLightPacket implements IMessage
    {
        public BlockPos pos;
        public EnumSkyBlock type;

        public UpdateLightPacket()
        {
            //Required
        }

        public UpdateLightPacket(BlockPos pos, EnumSkyBlock type)
        {
            this.pos = pos;
            this.type = type;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
            buf.writeBoolean(type == EnumSkyBlock.BLOCK);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            type = buf.readBoolean() ? EnumSkyBlock.BLOCK : EnumSkyBlock.SKY;
        }
    }

    public static class UpdateLightPacketHandler implements IMessageHandler<UpdateLightPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(UpdateLightPacket packet, MessageContext ctx)
        {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() ->
            {
                World world = mc.world;
                EnumSkyBlock type = packet.type;
                BlockPos pos = packet.pos;
                for (BlockPos involved : new BlockPos[]{pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.west(), pos.east()})
                {
                    world.checkLightFor(type, involved);
                }
            });
            return null;
        }
    }
}
