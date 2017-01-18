uniform float grad;

vec4 color(float value, float radius, float max) {
	return vec4(grad*value/max, grad*value/max, 0.5, 1.);
}