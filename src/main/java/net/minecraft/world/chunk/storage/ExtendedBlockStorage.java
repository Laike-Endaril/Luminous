package net.minecraft.world.chunk.storage;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.NibbleArray;

public class ExtendedBlockStorage
{
    private final int yBase;
    private int blockRefCount, tickRefCount;
    private final BlockStateContainer data;
    private NibbleArray blockLight;
    private NibbleArray skyLight;

    public ExtendedBlockStorage(int y, boolean storeSkylight)
    {
        yBase = y;
        data = new BlockStateContainer();
        blockLight = new NibbleArray();
        if (storeSkylight) skyLight = new NibbleArray();
    }

    public IBlockState get(int x, int y, int z)
    {
        return data.get(x, y, z);
    }

    public void set(int x, int y, int z, IBlockState newBlockState)
    {
        IBlockState oldBlockState = get(x, y, z);
        Block oldBlock = oldBlockState.getBlock();

        if (newBlockState instanceof net.minecraftforge.common.property.IExtendedBlockState) newBlockState = ((net.minecraftforge.common.property.IExtendedBlockState) newBlockState).getClean();
        Block newBlock = newBlockState.getBlock();

        if (oldBlock != Blocks.AIR)
        {
            --blockRefCount;
            if (oldBlock.getTickRandomly()) --tickRefCount;
        }

        if (newBlock != Blocks.AIR)
        {
            ++blockRefCount;
            if (newBlock.getTickRandomly()) ++tickRefCount;
        }

        data.set(x, y, z, newBlockState);
    }

    public boolean isEmpty()
    {
        return blockRefCount == 0;
    }

    public boolean needsRandomTick()
    {
        return tickRefCount > 0;
    }

    public int getYLocation()
    {
        return yBase;
    }

    public void setSkyLight(int x, int y, int z, int value)
    {
        skyLight.set(x, y, z, value);
    }

    public int getSkyLight(int x, int y, int z)
    {
        return skyLight.get(x, y, z);
    }

    public void setBlockLight(int x, int y, int z, int value)
    {
        blockLight.set(x, y, z, value);
    }

    public int getBlockLight(int x, int y, int z)
    {
        return blockLight.get(x, y, z);
    }

    public void recalculateRefCounts()
    {
        blockRefCount = 0;
        tickRefCount = 0;

        for (int i = 0; i < 16; ++i)
        {
            for (int j = 0; j < 16; ++j)
            {
                for (int k = 0; k < 16; ++k)
                {
                    Block block = get(i, j, k).getBlock();
                    if (block != Blocks.AIR)
                    {
                        ++blockRefCount;
                        if (block.getTickRandomly()) ++tickRefCount;
                    }
                }
            }
        }
    }

    public BlockStateContainer getData()
    {
        return data;
    }

    public NibbleArray getBlockLight()
    {
        return blockLight;
    }

    public NibbleArray getSkyLight()
    {
        return skyLight;
    }

    public void setBlockLight(NibbleArray newBlocklightArray)
    {
        blockLight = newBlocklightArray;
    }

    public void setSkyLight(NibbleArray newSkylightArray)
    {
        skyLight = newSkylightArray;
    }
}