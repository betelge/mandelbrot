vec3 hsv2rgb(vec3 c);

uniform float grad;
uniform vec3 col1;
uniform vec3 col2;

vec4 color(float value, float radius, float max) {
	float log2 = log(2.);
	
    float smooth = 1. - log( 0.5*log(radius) / log2 ) / log2;
	
	float speed2 = log(value + smooth);
	
	vec3 hsv = vec3(grad*speed2, 0.4, 1.);
	
	float mod = abs(mod(grad*speed2, 2.) - 1.);
	vec3 col = (1.-mod) * col1 + mod * col2;
	
	if(value == 0.) col = vec3(0.);
	
	return vec4(col, 1.);
}