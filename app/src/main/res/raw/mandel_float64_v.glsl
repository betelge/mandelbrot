#version 300 es
in vec3 position;

uniform vec2 scale;
uniform vec2 offset;

out vec4 c;
out vec2 uv;
out vec2 pos;

void main(void)
{
	pos = 2. * position.xy;
    uv = position.xy * .5 + .5;
	c.xz = 2.*(scale*position.xy + offset);
	c.yw = vec2(0.);
	gl_Position = vec4(position, 1.);
}