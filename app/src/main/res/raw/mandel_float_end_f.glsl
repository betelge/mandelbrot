#version 300 es
precision highp float;
precision highp int;

uniform float MAX_ITER;
uniform vec2 iteration;
uniform highp sampler2D tex; // Intermediate float texture

in vec2 uv;

out vec4 outColor;

vec4 color(float value, float radius, float max);

void main()
{
	vec3 z = texture(tex, uv).xyz;
		
	int i = int(z.z);
	float radius = z.x*z.x + z.y*z.y;
	float endIter = iteration.x + iteration.y;

	if(i == int(endIter) && i < int(MAX_ITER))
		discard;

	float speed2 = (i == int(MAX_ITER) ? 0.0 : float(i));

	outColor = color(speed2, radius, MAX_ITER);
}
