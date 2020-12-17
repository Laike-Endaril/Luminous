package com.fantasticsource.luminous;

import com.fantasticsource.luminous.asm.ClassMapper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

import static com.fantasticsource.luminous.asm.LuminousCore.*;

@Mod(modid = MODID, name = NAME, version = VERSION)
public class Luminous
{
    @Mod.EventHandler
    public static void temporary(FMLServerStoppedEvent event)
    {
        ClassMapper.save();
    }
}
