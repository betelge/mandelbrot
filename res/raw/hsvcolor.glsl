vec3 hsv2rgb(vec3 c);

vec4 color(float value, float radius, float max) {
	float log2 = log(2.);
	
    float smooth = 1. - log( 0.5*log(radius) / log2 ) / log2;
	
	float speed2 = log(value + smooth);
	
	vec3 hsv = vec3(speed2, 1., 1.);
	
	if(value == 0.) hsv.z = 0.;
	
	return vec4(hsv2rgb(hsv), 1.0);
}