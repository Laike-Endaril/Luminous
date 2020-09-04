package net.minecraft.network.play.server;

import com.fantasticsource.tools.Tools;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SPacketChunkData implements Packet<INetHandlerPlayClient>
{
    private int chunkX;
    private int chunkZ;
    private int availableSections;
    private byte[] buffer;
    private List<NBTTagCompound> tileEntityTags;
    private boolean fullChunk;

    public SPacketChunkData()
    {
    }

    public SPacketChunkData(Chunk chunkIn, int changedSectionFilter)
    {
        chunkX = chunkIn.x;
        chunkZ = chunkIn.z;
        fullChunk = changedSectionFilter == 65535;
        boolean flag = chunkIn.getWorld().provider.hasSkyLight();
        buffer = new byte[calculateChunkSize(chunkIn, flag, changedSectionFilter)];
        availableSections = extractChunkData(new PacketBuffer(getWriteBuffer()), chunkIn, flag, changedSectionFilter);
        tileEntityTags = Lists.newArrayList();

        for (Entry<BlockPos, TileEntity> entry : chunkIn.getTileEntityMap().entrySet())
        {
            BlockPos blockpos = entry.getKey();
            TileEntity tileentity = entry.getValue();
            int i = blockpos.getY() >> 4;

            if (isFullChunk() || (changedSectionFilter & 1 << i) != 0)
            {
                NBTTagCompound nbttagcompound = tileentity.getUpdateTag();
                tileEntityTags.add(nbttagcompound);
            }
        }
    }

    public void readPacketData(PacketBuffer buf) throws IOException
    {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
        fullChunk = buf.readBoolean();
        availableSections = buf.readVarInt();
        int i = buf.readVarInt();

        if (i > 2097152)
        {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        }
        else
        {
            buffer = new byte[i];
            buf.readBytes(buffer);
            int j = buf.readVarInt();
            tileEntityTags = Lists.<NBTTagCompound>newArrayList();

            for (int k = 0; k < j; ++k)
            {
                tileEntityTags.add(buf.readCompoundTag());
            }
        }
    }

    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeBoolean(fullChunk);
        buf.writeVarInt(availableSections);
        buf.writeVarInt(buffer.length);
        buf.writeBytes(buffer);
        buf.writeVarInt(tileEntityTags.size());

        for (NBTTagCompound nbttagcompound : tileEntityTags)
        {
            buf.writeCompoundTag(nbttagcompound);
        }
    }

    public void processPacket(INetHandlerPlayClient handler)
    {
        handler.handleChunkData(this);
    }

    @SideOnly(Side.CLIENT)
    public PacketBuffer getReadBuffer()
    {
        return new PacketBuffer(Unpooled.wrappedBuffer(buffer));
    }

    private ByteBuf getWriteBuffer()
    {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(buffer);
        bytebuf.writerIndex(0);
        return bytebuf;
    }

    public int extractChunkData(PacketBuffer buf, Chunk chunkIn, boolean writeSkylight, int changedSectionFilter)
    {
        int availableFlags = 0;
        ExtendedBlockStorage[] blockStorageArray = chunkIn.getBlockStorageArray();

        for (int i = 0; i < blockStorageArray.length; ++i)
        {
            ExtendedBlockStorage extendedblockstorage = blockStorageArray[i];

            if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!isFullChunk() || !extendedblockstorage.isEmpty()) && (changedSectionFilter & 1 << i) != 0)
            {
                availableFlags |= 1 << i;
                extendedblockstorage.getData().write(buf);


                //Luminous start
                byte[] lightBytes = extendedblockstorage.getBlockLight().getData(), alteredLightBytes = new byte[lightBytes.length];
                System.arraycopy(lightBytes, 0, alteredLightBytes, 0, lightBytes.length);
                NibbleArray alteredNibbleArray = new NibbleArray(alteredLightBytes);
                for (Map.Entry<BlockPos, Integer> entry : chunkIn.blockLightOverrides.entrySet())
                {
                    BlockPos pos = entry.getKey();
                    if (pos.getY() < extendedblockstorage.getYLocation() || pos.getY() >= extendedblockstorage.getYLocation() + 16) continue;

                    alteredNibbleArray.set(Tools.posMod(pos.getX(), 16), Tools.posMod(pos.getY(), 16), Tools.posMod(pos.getZ(), 16), entry.getValue());
                }
                buf.writeBytes(alteredLightBytes);
                //Luminous end


                if (writeSkylight)
                {
                    //Luminous start
                    lightBytes = extendedblockstorage.getSkyLight().getData();
                    System.arraycopy(lightBytes, 0, alteredLightBytes, 0, lightBytes.length);
                    for (Map.Entry<BlockPos, Integer> entry : chunkIn.skyLightOverrides.entrySet())
                    {
                        BlockPos pos = entry.getKey();
                        if (pos.getY() < extendedblockstorage.getYLocation() || pos.getY() >= extendedblockstorage.getYLocation() + 16) continue;

                        alteredNibbleArray.set(Tools.posMod(pos.getX(), 16), Tools.posMod(pos.getY(), 16), Tools.posMod(pos.getZ(), 16), entry.getValue());
                    }
                    buf.writeBytes(alteredLightBytes);
                    //Luminous end
                }
            }
        }

        if (isFullChunk())
        {
            buf.writeBytes(chunkIn.getBiomeArray());
        }

        return availableFlags;
    }

    protected int calculateChunkSize(Chunk chunkIn, boolean p_189556_2_, int p_189556_3_)
    {
        int availableDataSize = 0;
        ExtendedBlockStorage[] blockStorageArray = chunkIn.getBlockStorageArray();

        for (int j = 0; j < blockStorageArray.length; ++j)
        {
            ExtendedBlockStorage extendedblockstorage = blockStorageArray[j];

            if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE && (!isFullChunk() || !extendedblockstorage.isEmpty()) && (p_189556_3_ & 1 << j) != 0)
            {
                availableDataSize = availableDataSize + extendedblockstorage.getData().getSerializedSize();
                availableDataSize = availableDataSize + extendedblockstorage.getBlockLight().getData().length;

                if (p_189556_2_)
                {
                    availableDataSize += extendedblockstorage.getSkyLight().getData().length;
                }
            }
        }

        if (isFullChunk()) availableDataSize += chunkIn.getBiomeArray().length;

        return availableDataSize;
    }

    @SideOnly(Side.CLIENT)
    public int getChunkX()
    {
        return chunkX;
    }

    @SideOnly(Side.CLIENT)
    public int getChunkZ()
    {
        return chunkZ;
    }

    @SideOnly(Side.CLIENT)
    public int getExtractedSize()
    {
        return availableSections;
    }

    public boolean isFullChunk()
    {
        return fullChunk;
    }

    @SideOnly(Side.CLIENT)
    public List<NBTTagCompound> getTileEntityTags()
    {
        return tileEntityTags;
    }
}