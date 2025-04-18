#version 330 core

//From outside world
uniform sampler2D uniform_texture_sampler;
uniform sampler2D uniform_lightmap_sampler;

//From previous shaders
in vec4 color_blend;
in vec2 texture_uv;
in vec2 lightmap_uv;

void main (void)
{
    //Output to pipeline
    gl_FragColor = texture2D(uniform_texture_sampler, texture_uv);
}
