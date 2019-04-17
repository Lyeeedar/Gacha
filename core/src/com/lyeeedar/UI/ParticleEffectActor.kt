package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.Colour

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

	val tempCol = Colour()
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		tempCol.set(color.r, color.g, color.b, color.a * parentAlpha)
		effect.render(batch!!, x, y, 1f, tempCol, width, height, 0f, 0f, 0, 0, false)

		super.draw(batch, parentAlpha)
	}
}