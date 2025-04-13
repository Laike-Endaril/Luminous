#version 330 core

//Linked to outside world
//Match format from DefaultVertexFormats.BLOCK
layout(location = 0) in vec3 in_position;
layout(location = 3) in uvec4 in_color;
layout(location = 7) in vec2 in_texture_uv;
layout(location = 9) in vec2 in_lightmap_uv;
uniform mat4 uniform_model_view;
uniform mat4 uniform_projection;

//Linked to other shaders
//Outputs will automatically be interpolated before they reach the fragment shader
out uvec4 color_blend;
out vec2 texture_uv;
out vec2 lightmap_uv;



void main(void)
{
    //Output to pipeline
    gl_Position = uniform_projection * uniform_model_view * vec4(in_position, 1);

    //Output to next shader(s) (is this necessary, or could I just use "layout in" in the fragment shader?)
    color_blend = in_color;
    texture_uv = in_texture_uv;
    lightmap_uv = in_lightmap_uv;
}
