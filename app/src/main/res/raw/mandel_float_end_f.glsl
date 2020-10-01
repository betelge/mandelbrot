precision highp float;

uniform float MAX_ITER;
uniform vec2 iteration;
uniform highp sampler2D tex; // Intermediate float texture

varying vec2 uv;

vec4 color(float value, float radius, float max);

void main()
{
	vec3 z = texture2D(tex, uv).xyz;
		
	int i = int(z.z);
	float radius = z.x*z.x + z.y*z.y;
	float endIter = iteration.x + iteration.y;

	if(i == int(endIter) && i < int(MAX_ITER))
	discard;

	float speed2 = (i == int(MAX_ITER) ? 0.0 : float(i));

	gl_FragColor = color(speed2, radius, MAX_ITER);
}
