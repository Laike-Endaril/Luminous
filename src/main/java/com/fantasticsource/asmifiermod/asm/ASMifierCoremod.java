package com.fantasticsource.asmifiermod.asm;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.versioning.VersionParser;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

@IFMLLoadingPlugin.Name(ASMifierCoremod.NAME)
@IFMLLoadingPlugin.MCVersion(Loader.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions("com.fantasticsource.asmifiermod.asm")
public class ASMifierCoremod implements IFMLLoadingPlugin
{
    public static final String MODID = "asmifiermod";
    public static final String NAME = "ASMifier Mod";
    public static final String VERSION = "1.12.2.000";

    public static final HashSet<String> FULL_INPUT_CLASSNAMES = new HashSet<>();

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{"com.fantasticsource.asmifiermod.asm.ASMifierTransformer"};
    }

    @Override
    public String getModContainerClass()
    {
        return "com.fantasticsource.asmifiermod.asm.ASMifierCoremod$CoreModContainer";
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public static final class CoreModContainer extends DummyModContainer
    {
        public CoreModContainer()
        {
            super(new ModMetadata());
            ModMetadata meta = getMetadata();
            meta.modId = MODID + "core";
            meta.name = NAME + " Coremod";
            meta.version = VERSION;
            meta.description = "Outputs ASM code dumps to use for ASM editing";
            meta.authorList.add("Laike Endaril");


            try
            {
                File configFile = new File("config" + File.separator + meta.modId + ".txt");
                if (!configFile.exists()) configFile.createNewFile();
                else
                {
                    BufferedReader br = new BufferedReader(new FileReader(configFile));

                    String line = br.readLine();
                    while (line != null)
                    {
                        FULL_INPUT_CLASSNAMES.add(line.trim());
                        line = br.readLine();
                    }

                    br.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            System.out.println(meta.name + " initialized");
        }

        @Override
        public VersionRange acceptableMinecraftVersionRange()
        {
            return VersionParser.parseRange("[1.12,1.13)");
        }

        @Override
        public ModContainer getMod()
        {
            return this;
        }

        @Override
        public boolean registerBus(EventBus bus, LoadController controller)
        {
            // required for error handling
            return true;
        }
    }
}