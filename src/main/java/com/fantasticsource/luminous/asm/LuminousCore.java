package com.fantasticsource.luminous.asm;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.versioning.VersionParser;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name(LuminousCore.NAME)
@IFMLLoadingPlugin.MCVersion(Loader.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions("com.fantasticsource.luminous.asm")
public class LuminousCore implements IFMLLoadingPlugin
{
    public static final String MODID = "luminous";
    public static final String NAME = "Luminous";
    public static final String VERSION = "1.12.2.000";

    public LuminousCore()
    {
        System.out.println("Luminous Coremod initialized");
    }

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{"com.fantasticsource.luminous.asm.LuminousTransformer"};
    }

    @Override
    public String getModContainerClass()
    {
        return "com.fantasticsource.luminous.asm.LuminousCore$CoreModContainer";
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