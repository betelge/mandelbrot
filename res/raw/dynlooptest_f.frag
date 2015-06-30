precision mediump float;

uniform float MAX_ITER;

vec2 c = vec2(0.1, 0.2);

vec2 iter(vec2 z)
{
	return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
}

void main()
{

	// (c.x, c.y, iteration)
	vec2 z = vec2(0.0, 0.0);
		
	int max = int(MAX_ITER);
	float radius = 0.;
	for( i=0; i<max; i++ ) {
		radius = z.x*z.x + z.y*z.y;
		if( radius > 16.) break;
		z = iter(z);
	}
	
	float speed2 = (i == max ? 0.0 : float(i));
	
	gl_FragColor = vec4(speed2, radius, 0., 1.);