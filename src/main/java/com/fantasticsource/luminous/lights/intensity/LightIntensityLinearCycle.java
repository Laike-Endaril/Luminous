package com.fantasticsource.luminous.lights.intensity;

public class LightIntensityLinearCycle extends LightIntensity
{
    public int min, max, currentTick, cycleTicks;

    public LightIntensityLinearCycle(int min, int max, int cycleTicks)
    {
        this(min, max, cycleTicks, 0);
    }

    public LightIntensityLinearCycle(int min, int max, int cycleTicks, int currentTick)
    {
        this.min = min;
        this.max = max;
        this.cycleTicks = cycleTicks;

        this.currentTick = currentTick;
    }

    @Override
    public int intensity()
    {
        return min + (int) ((max - min) * (1 - (double) Math.abs(currentTick - (cycleTicks >>> 1)) / (cycleTicks >>> 1)));
    }

    @Override
    public void tick()
    {
        currentTick = (currentTick + 1) % cycleTicks;
    }
}
