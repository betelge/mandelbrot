#version 300 es

#pragma optionNV(fastmath off)
#pragma optionNV(fastprecision off)

precision highp float;

uniform float MAX_ITER;
uniform vec2 iteration; // (iteration, steps) // TODO: uvec2?
uniform highp sampler2D tex; // Intermediate float texture
uniform highp sampler2D counterTex;
uniform float split;

uniform vec2 offset;
uniform vec2 offsetFine;
uniform vec2 scale;

in vec2 pos;
in vec2 uv;

layout(location = 0) out highp vec4 value;
layout(location = 1) out highp float counter;

vec2 splitf(float a);

vec2 add (vec2 dsa, vec2 dsb);
vec2 sub (vec2 dsa, vec2 dsb);
vec2 mul (vec2 dsa, vec2 dsb);

vec4 iter(vec4 z, vec4 c)
{
	//return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + 2.0*c;


	vec2 imag = mul(z.xy, z.zw);
	imag = add(imag, imag);
	vec4 result = vec4(sub(mul(z.xy, z.xy), mul(z.zw, z.zw)), imag);

	result.xy = add(result.xy, c.xy);
	result.zw = add(result.zw, c.zw);

	return result;
}

void main()
{
	// Calculate c

	vec2 posX = /*vec2(pos.x,0.);*/splitf(pos.x);
	vec2 posY = /*vec2(pos.y,0.);*/splitf(pos.y);

	vec2 scaX = /*vec2(scale.x,0.);*/splitf(scale.x);
	vec2 scaY = /*vec2(scale.y,0.);*/splitf(scale.y);

	vec2 offX = vec2(2.*offset.x,2.*offsetFine.x);//splitf(2.0*offset.x);
	vec2 offY = vec2(2.*offset.y,2.*offsetFine.y);//splitf(2.0*offset.y);

	vec2 real = mul(scaX, posX);
	real = add(real, offX);

	vec2 imag = mul(scaY, posY);
	imag = add(imag, offY);

	vec4 c = vec4(real, imag);


	vec4 z = texture(tex, uv);

	uint start = uint(iteration.x);
	uint steps = uint(iteration.y);
		
	uint i = uint(texture(counterTex, uv).x);
	if(i == start) {

		uint max = uint(MAX_ITER);
		vec2 radius2 = vec2(0.);
		for (i = start; i < max && i < start + steps; i++) {
			//radius2 = z.x*z.x + z.y*z.y;
			radius2 = add(mul(z.xy,z.xy), mul(z.zw,z.zw));
			if (radius2.x > 16.) break;// TODO: Is it faster to ignore this?
			z = iter(z, c);
		}

	}
	
	value = z;
	counter = float(i);
}
