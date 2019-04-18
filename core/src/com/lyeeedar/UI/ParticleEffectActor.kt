package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.lerp

class ParticleEffectActor(val effect: ParticleEffect, val removeOnCompletion: Boolean) : Widget()
{
	val shape: ShapeRenderer by lazy { ShapeRenderer() }

	var acted = false
	override fun act(delta: Float)
	{
		acted = true

		var lx = x + width / 2
		var ly = y + height / 2

		if (effect.lockPosition)
		{

		}
		else
		{
			if (effect.facing.x != 0)
			{
				lx = x + effect.size[1].toFloat() * 0.5f * width
				ly = y + effect.size[0].toFloat() * 0.5f * height
			}
			else
			{
				if (effect.isCentered)
				{
					lx = x + 0.5f * width
					ly = y + 0.5f * height
				}
				else
				{
					lx = x + effect.size[0].toFloat() * 0.5f * width
					ly = y + effect.size[1].toFloat() * 0.5f * height
				}
			}

			effect.setPosition(lx / width, ly / height)
		}

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

		super.act(delta)
	}

	val tempVec = Vector2()
	val tempCol = Colour()
	val tempCol2 = Colour()
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (!acted)
		{
			act(0f)
		}

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val actorCol = tempCol2.set(color.r, color.g, color.b, color.a * parentAlpha)
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

					col.mul(actorCol).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					val drawx = tempVec.x + px
					val drawy = tempVec.y + py

					batch.setColor(col.toFloatBits())
					batch.draw(tex1.second, drawx * width - sizex*0.5f, drawy * height - sizey*0.5f, sizex*0.5f, sizey*0.5f, sizex, sizey, 1f, 1f, rotation)
				}
			}
		}

		if (debug)
		{
			shape.projectionMatrix = stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			effect.debug(shape, 0f, 0f, width, true, true, true)

			shape.end()
		}

		super.draw(batch, parentAlpha)
	}
}