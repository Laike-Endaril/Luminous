#version 120

//From outside world
//Layout matches format from DefaultVertexFormats.BLOCK
//layout(location = 0) in vec3 in_position;
//layout(location = 1) in uvec4 in_color;
//layout(location = 2) in vec2 in_texture_uv;
//layout(location = 3) in vec2 in_lightmap_uv;
uniform mat4 uniform_model_view;
uniform mat4 uniform_projection;

//To next shaders
//Outputs will automatically be interpolated before they reach the fragment shader
varying out vec4 color_blend;
varying out vec2 texture_uv;
varying out vec2 lightmap_uv;



void main(void)
{
    //Output to pipeline
    gl_Position = uniform_projection * uniform_model_view * gl_Vertex;

    //Output to next shader(s) (is this necessary for the pass-throughs with no calcs, or could I just use "layout in" in the fragment shader?)
//    color_blend = in_color / vec4(255, 255, 255, 255);
//    texture_uv = in_texture_uv;
    texture_uv = gl_MultiTexCoord0.st;
//    lightmap_uv = in_lightmap_uv;
}
