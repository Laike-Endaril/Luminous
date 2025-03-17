package com.fantasticsource.luminous.asm;

import com.fantasticsource.luminous.asm.classtransformers.BlockStateContainerDump;
import com.fantasticsource.luminous.asm.classtransformers.ChunkDump;
import com.fantasticsource.luminous.asm.classtransformers.WorldDump;
import net.minecraft.launchwrapper.IClassTransformer;

public class LuminousTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (name.equals(transformedName)) return bytes;

        switch (transformedName)
        {
            case "net.minecraft.world.World":
                return WorldDump.dump();

            case "net.minecraft.world.chunk.Chunk":
                return ChunkDump.dump();

            case "net.minecraft.block.state.BlockStateContainer":
                return BlockStateContainerDump.dump();
        }

        return bytes;
    }
}
