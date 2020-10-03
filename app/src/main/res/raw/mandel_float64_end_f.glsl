#version 300 es
precision highp float;

uniform float MAX_ITER;
uniform vec2 iteration;
uniform highp sampler2D tex; // Intermediate float texture
uniform highp sampler2D counterTex;
uniform float split;

in vec2 uv;

out vec4 outColor;

vec4 color(float value, float radius, float max);

vec2 splitf(float a);

vec2 add (vec2 dsa, vec2 dsb);
vec2 sub (vec2 dsa, vec2 dsb);
vec2 mul (vec2 dsa, vec2 dsb);

void main()
{
	vec4 z = texture(tex, uv);
		
	uint i = uint(texture(counterTex, uv).x);
	vec2 radius2 = add(mul(z.xy,z.xy), mul(z.zw,z.zw));
	float endIter = iteration.x + iteration.y;

	if(i == uint(endIter) && i < uint(MAX_ITER))
		discard;

	float speed2 = (i == uint(MAX_ITER) ? 0.0 : float(i));

	outColor = color(speed2, radius2.x, MAX_ITER);
}
