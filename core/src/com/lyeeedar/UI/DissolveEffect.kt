package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.lyeeedar.Util.AssetManager

class DissolveEffect(val drawable: Drawable, val duration: Float) : Actor()
{

	var time = 0f

	override fun act(delta: Float)
	{
		time += delta
		if (time >= duration)
		{
			remove()
		}
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.shader = shader

		shader.setUniformf("u_timeAlpha", time / duration)
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
		drawable.draw(batch, x, y, width, height)

		batch.shader = null
	}

	companion object
	{
		val gradient = AssetManager.loadTextureRegion("GUI/burngradient")!!
		val shader = createShader()

		fun createShader(): ShaderProgram
		{
			val vertexShader = """

attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_pos;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

	v_pos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy;

	gl_Position = u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
			val fragmentShader = """

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

uniform float u_timeAlpha;

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_pos;

uniform sampler2D u_texture;

vec4 gradientTexCoords = vec4(${gradient.u}, ${gradient.v}, ${gradient.u2}, ${gradient.v2});

float hash(float n) { return fract(sin(n) * 1e4); }
float hash(vec2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }

float noise(float x) {
	float i = floor(x);
	float f = fract(x);
	float u = f * f * (3.0 - 2.0 * f);
	return mix(hash(i), hash(i + 1.0), u);
}

float noise(vec2 x) {
	vec2 i = floor(x);
	vec2 f = fract(x);

	// Four corners in 2D of a tile
	float a = hash(i);
	float b = hash(i + vec2(1.0, 0.0));
	float c = hash(i + vec2(0.0, 1.0));
	float d = hash(i + vec2(1.0, 1.0));

	// Simple 2D lerp using smoothstep envelope between the values.
	// return vec3(mix(mix(a, b, smoothstep(0.0, 1.0, f.x)),
	//			mix(c, d, smoothstep(0.0, 1.0, f.x)),
	//			smoothstep(0.0, 1.0, f.y)));

	// Same code, with the clamps in smoothstep and common subexpressions
	// optimized away.
	vec2 u = f * f * (3.0 - 2.0 * f);
	return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main()
{
	vec4 diffuseSample = texture2D(u_texture, v_texCoords);

	float noiseVal = noise(v_pos / 5.0 + v_texCoords);
	float diff = noiseVal - u_timeAlpha;

	if (diff < 0.0)
	{
		diffuseSample.a = 0.0;
	}
	else if (diff < 0.4)
	{
		vec4 gradientSample = texture2D(u_texture, mix(gradientTexCoords.xy, gradientTexCoords.zw, diff / 0.5));

		float alpha = gradientSample.a;
		gradientSample.a = diffuseSample.a;

		diffuseSample = mix(diffuseSample, gradientSample, alpha);
	}

	gl_FragColor = v_color * diffuseSample;
}

"""

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)
			return shader
		}
	}
}