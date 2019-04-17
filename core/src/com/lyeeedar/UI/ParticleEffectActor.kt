package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.lerp

class ParticleEffectActor(val effect: ParticleEffect, val removeOnCompletion: Boolean) : Widget()
{
	override fun act(delta: Float)
	{
		effect.update(delta)

		if (effect.completed && effect.complete())
		{
			if (removeOnCompletion)
			{
				remove()
			}
			else
			{
				effect.start()
			}
		}

		super.act(delta)
	}

	val tempVec = Vector2()
	val tempCol = Colour()
	val tempCol2 = Colour()
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		tempCol2.set(color.r, color.g, color.b, color.a * parentAlpha)
		var lx = x
		var ly = y

		if (effect.lockPosition)
		{

		}
		else
		{
			if (effect.facing.x != 0)
			{
				lx = x + effect.size[1].toFloat() * 0.5f
				ly = y + effect.size[0].toFloat() * 0.5f
			}
			else
			{
				if (effect.isCentered)
				{
					lx = x + 0.5f
					ly = y + 0.5f
				}
				else
				{
					lx = x + effect.size[0].toFloat() * 0.5f
					ly = y + effect.size[1].toFloat() * 0.5f
				}
			}

			effect.setPosition(lx, ly)
		}

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val posOffset = effect.animation?.renderOffset(false)
		lx += (posOffset?.get(0) ?: 0f)
		ly += (posOffset?.get(1) ?: 0f)

		if (effect.faceInMoveDirection)
		{
			val angle = com.lyeeedar.Util.getRotation(effect.lastPos, tempVec.set(lx, ly))
			effect.rotation = angle
			effect.lastPos.set(lx, ly)
		}
		else
		{
			effect.rotation = rotation
		}

		val animCol = effect.animation?.renderColour() ?: Colour.WHITE

		for (emitter in effect.emitters)
		{
			val emitterOffset = emitter.keyframe1.offset.lerp(emitter.keyframe2.offset, emitter.keyframeAlpha)

			for (particle in emitter.particles)
			{
				var px = 0f
				var py = 0f

				if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
				{
					tempVec.set(emitterOffset)
					tempVec.scl(emitter.size)
					tempVec.rotate(emitter.rotation)

					px += (emitter.position.x + tempVec.x)
					py += (emitter.position.y + tempVec.y)
				}

				for (pdata in particle.particles)
				{
					val keyframe1 = pdata.keyframe1
					val keyframe2 = pdata.keyframe2
					val alpha = pdata.keyframeAlpha

					val tex1 = keyframe1.texture[pdata.texStream]

					val col = tempCol.set(keyframe1.colour[pdata.colStream]).lerp(keyframe2.colour[pdata.colStream], alpha)
					col.a = keyframe1.alpha[pdata.alphaStream].lerp(keyframe2.alpha[pdata.alphaStream], alpha)

					val size = keyframe1.size[pdata.sizeStream].lerp(keyframe2.size[pdata.sizeStream], alpha, pdata.ranVal)
					var sizex = size * width
					var sizey = size * height

					if (particle.allowResize)
					{
						sizex *= emitter.size.x
						sizey *= emitter.size.y
					}

					val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

					col.mul(tempCol2).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					val drawx = tempVec.x + px
					val drawy = tempVec.y + py

					batch!!.draw(tex1.second, drawx, drawy, sizex*0.5f, sizey*0.5f, sizex, sizey, 1f, 1f, rotation)
				}
			}
		}

		super.draw(batch, parentAlpha)
	}
}