#version 330 core

//Linked to outside world
out vec4 fragdata_color;

void main (void)
{
    //Out to fragdata location
    fragdata_color = vec4(1, 0, 0, 1);
}
