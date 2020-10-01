#version 300 es
in vec3 position;

uniform vec2 scale;
uniform vec2 offset;

out vec2 c;
out vec2 uv;

void main(void)
{
    uv = position.xy * .5 + .5;
	c = 2.*(scale*position.xy + offset);
	gl_Position = vec4(position, 1.);
}