#version 300 es
precision highp float;

uniform float MAX_ITER;
uniform vec2 iteration; // (iteration, steps)
uniform highp sampler2D tex; // Intermediate float texture

in highp vec2 c;
in vec2 uv;

out highp vec4 outColor;

vec2 iter(vec2 z)
{
	return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
}

void main()
{

	vec3 inter = texture(tex, uv).xyz;
	vec2 z = inter.xy;

	int start = int(iteration.x);
	int steps = int(iteration.y);
		
	int i = int(inter.z);
	if(i == start) {

		int max = int(MAX_ITER);
		float radius = 0.;
		for (i = start; i < max && i < start + steps; i++) {
			radius = z.x*z.x + z.y*z.y;
			if (radius > 16.) break;// TODO: Is it faster to ignore this?
			z = iter(z);
		}

	}
	
	outColor = vec4(z, float(i), 0.);
}
