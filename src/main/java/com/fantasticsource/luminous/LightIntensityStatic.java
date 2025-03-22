package com.fantasticsource.luminous;

public class LightIntensityStatic extends LightIntensity
{
    public int intensity;

    public LightIntensityStatic(int intensity)
    {
        this.intensity = intensity;
    }

    @Override
    public int intensity()
    {
        return intensity;
    }
}
