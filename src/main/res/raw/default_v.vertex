
uniform mat4 modelViewMatrix;
uniform mat4 perspectiveMatrix;
uniform mat3 normalMatrix;

attribute vec3 position;
attribute vec2 textureCoord;
attribute vec3 normal;

varying vec3 pos;
varying vec3 modelPos;
varying vec2 tc;
varying vec3 N;
varying vec4 col;

void main()
{
	tc = textureCoord;
	col = vec4(normal, 1.0);
	N = normalMatrix * normal;
	
	modelPos = position;
	vec4 posi = modelViewMatrix * vec4(position, 1.0);
	pos = posi.xyz/posi.w;
	gl_Position = perspectiveMatrix * posi;	
}
