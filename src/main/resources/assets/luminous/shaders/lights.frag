#version 120

//From outside world
uniform sampler2D uniform_texture_sampler;
uniform sampler2D uniform_lightmap_sampler;

void main (void)
{
    //Output to pipeline
    gl_FragColor = texture2D(uniform_texture_sampler, gl_TexCoord[0].xy) * gl_Color * texture2D(uniform_lightmap_sampler, (gl_TexCoord[1].xy + vec2(8, 8)) / vec2(256, 256));
}
