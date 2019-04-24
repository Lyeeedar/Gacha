package com.lyeeedar.Util

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Components.PositionComponent
import com.lyeeedar.Components.RenderableComponent
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import ktx.math.times

class BloodSplatter
{
	companion object
	{
		val splatters = AssetManager.loadSprite("Oryx/Custom/terrain/bloodsplatter")

		fun splatter(source: Point, target: Point, emitDist: Float, angle: Float = 45f)
		{
			if (Global.resolveInstant) return

			var vector = target.toVec()
			vector.sub(source.x.toFloat(), source.y.toFloat())
			vector.nor()
			vector = vector.rotate(Random.random(-angle, angle))

			val dist = Random.randomWeighted() * emitDist
			vector = vector.scl(dist)

			val chosen = splatters.textures.random()

			val sprite = Sprite(chosen)
			sprite.baseScale[0] = 0.3f + Random.random(0.3f)
			sprite.baseScale[1] = sprite.baseScale[0]
			sprite.rotation = Random.random(180f)
			sprite.colour = Colour(1f, 0.6f + Random.random(0.2f), 0.6f + Random.random(0.2f), 0.4f + Random.random(0.2f))
			sprite.animationBlocksUpdate = false

			val renderable = RenderableComponent.obtain().set(sprite)
			val pos = PositionComponent.obtain()
			pos.position = target.copy()
			pos.offset.set(vector)

			val animDur = 0.1f + 0.15f * dist
			sprite.animation = LeapAnimation.obtain().set(animDur, arrayOf(vector * -1f, Vector2()), 0.1f + 0.1f * dist)

			val newEntity = EntityPool.obtain()
			newEntity.add(renderable)
			newEntity.add(pos)
			Global.engine.addEntity(newEntity)
		}
	}
}