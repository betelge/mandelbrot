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
		
	uint i = uint(z.z);
	float radius = z.x*z.x + z.y*z.y;
	float endIter = iteration.x + iteration.y;

	if(i == uint(endIter) && i < uint(MAX_ITER)) {
		outColor = vec4(0., 0., 0., endIter / MAX_ITER);
		return;
	}

	float speed2 = (i == uint(MAX_ITER) ? 0.0 : float(i));

	outColor = color(speed2, radius, MAX_ITER);
}
