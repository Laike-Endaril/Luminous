#version 330 core

//Linked to outside world
out vec4 fragdata_color;

uniform sampler2D uniform_sampler;


//Linked to other shaders
//Inputs are automatically interpolated before reaching the fragment shader
in vec2 texture_uv;
in float fade;
in vec3 color_blend;//TODO Not yet implemented in fragment shader or outside of shaders



void main (void)
{
    //Out to fragdata location
    vec4 tex_color = texture(uniform_sampler, texture_uv);
    tex_color.w += fade;
    if (tex_color.w > 1) tex_color.w = 1;
    fragdata_color = tex_color - vec4(1, 1, 1, 0) * fade;
}
