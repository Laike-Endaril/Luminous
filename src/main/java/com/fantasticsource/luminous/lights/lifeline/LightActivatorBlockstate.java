package com.fantasticsource.luminous.lights.lifeline;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

public class LightActivatorBlockstate extends LightActivator<IBlockState>
{
    public final WorldServer world;
    public final BlockPos pos;

    public LightActivatorBlockstate(WorldServer world, BlockPos pos, Predicate<IBlockState> predicate)
    {
        super(predicate);
        this.world = world;
        this.pos = pos;
    }

    @Override
    public boolean active()
    {
        return predicate.test(world.getBlockState(pos));
    }
}
