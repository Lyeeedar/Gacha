package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Sprite.DirectionalSprite

class DirectionalSpriteSystem(): AbstractSystem(Family.all(DirectionalSpriteComponent::class.java, PositionComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		for (entity in entities)
		{
			processEntity(entity, deltaTime)
		}
	}

	fun processEntity(entity: Entity?, deltaTime: Float)
	{
		if (entity == null) return

		val pos = entity.pos()
		val dirSprite = entity.directionalSprite()!!

		if (!dirSprite.directionalSprite.hasAnim(dirSprite.currentAnim))
		{
			dirSprite.currentAnim = "idle"
		}

		var renderable = entity.renderableOrNull()
		if (renderable == null)
		{
			val chosen = dirSprite.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)

			renderable = RenderableComponent.obtain().set(chosen)
			entity.add(renderable)
		}

		if (renderable.lockRenderable) return

		if (pos.facing == Direction.SOUTH)
		{
			dirSprite.lastV = DirectionalSprite.VDir.DOWN
		}
		else if (pos.facing == Direction.NORTH)
		{
			dirSprite.lastV = DirectionalSprite.VDir.UP
		}
		else if (pos.facing == Direction.EAST)
		{
			dirSprite.lastH = DirectionalSprite.HDir.RIGHT
		}
		else if (pos.facing == Direction.WEST)
		{
			dirSprite.lastH = DirectionalSprite.HDir.LEFT
		}

		val chosen = dirSprite.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)

		if (chosen != renderable.renderable)
		{
			chosen.animation = renderable.renderable.animation
			renderable.renderable.animation = null
			renderable.renderable = chosen
		}
	}
}