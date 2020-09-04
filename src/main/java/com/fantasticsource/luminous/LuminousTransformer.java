package com.fantasticsource.luminous;

import com.fantasticsource.mctools.MCTools;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.fantasticsource.luminous.Luminous.MODID;

public class LuminousTransformer implements IClassTransformer
{
    private static final ArrayList<String> REPLACEMENTS = new ArrayList<>(Arrays.asList(
            "net.minecraft.world.chunk.Chunk",
            "net.minecraft.network.play.server.SPacketChunkData"
    ));

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (REPLACEMENTS.contains(transformedName))
        {
            String name2 = MODID + "/tweakedclasses/" + transformedName.replaceAll("[.]()", "/") + ".class";
            InputStream stream = MCTools.getJarResourceStream(LuminousTransformer.class, name2);
            if (stream == null)
            {
                System.err.println(TextFormatting.RED + "Resource not found: " + name2);
                return bytes;
            }
            byte[] buf = new byte[10000];
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
