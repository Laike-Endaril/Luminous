#version 330 core

//Linked to outside world
in vec3 attrib_position;
in vec2 attrib_texture_coords;
in float attrib_fade;
in vec3 attrib_color; //TODO Not yet implemented in fragment shader or outside of shaders
in vec3 attrib_normals; //TODO Not yet implemented inside or outside of shaders

uniform mat4 uniform_mvp;



//Linked to other shaders
//Outputs will automatically be interpolated before they reach the fragment shader
out vec2 texture_uv;
out float fade;
out vec3 color_blend;



void main(void)
{
    //Output to pipeline
    gl_Position = uniform_mvp * vec4(attrib_position, 1);

    //Output to next shader(s)
    texture_uv = attrib_texture_coords;
    fade = attrib_fade;
    color_blend = attrib_color;
}
