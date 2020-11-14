package com.fantasticsource.luminous.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.fantasticsource.luminous.asm.LuminousCore.MODID;

public class LuminousTransformer implements IClassTransformer
{
    public static final ArrayList<String> REPLACEMENTS = new ArrayList<>(Arrays.asList(
            "net.minecraft.block.state.BlockStateContainer",
            "net.minecraft.world.chunk.Chunk",
            "net.minecraft.world.World"
    ));

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (REPLACEMENTS.contains(transformedName))
        {
            System.out.println(TextFormatting.AQUA + "Replacing " + transformedName);
            String name2 = "assets/" + MODID + "/tweakedclasses/" + transformedName.replaceAll("[.]", "/") + ".class";
            InputStream stream = LuminousTransformer.class.getClassLoader().getResourceAsStream(name2);
            if (stream == null)
            {
                throw new IllegalStateException(TextFormatting.RED + "Resource not found: " + name2);
            }
            byte[] buf = new byte[1024 * 1024];
            try
            {
                bytes = new byte[stream.read(buf)];
                System.arraycopy(buf, 0, bytes, 0, bytes.length);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
