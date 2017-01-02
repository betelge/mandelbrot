float rgb2float(vec3 color) {
	return (color.r + (color.g*32.) + (color.b*32.*32.)) / (32.*32.);
}

vec3 float2rgb(float f) { 
	float b = floor(f * 32.);
	float g = floor(f * 32.*32.) - (b*32.);
	float r = (floor(f * 32.*32.*32.) - (b*32.*32.)) - (g*32.);
	return vec3(r, g, b)/ 32.0;
}