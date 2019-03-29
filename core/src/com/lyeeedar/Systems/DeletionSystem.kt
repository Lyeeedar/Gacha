package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*

class DeletionSystem : AbstractSystem(Family.all(MarkedForDeletionComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		deletedEntities.clear()

		for (entity in entities)
		{
			processEntity(entity)
		}
	}

	val deletedEntities = ObjectSet<Entity>()

	fun processEntity(entity: Entity)
	{
		if (deletedEntities.contains(entity)) return
		deletedEntities.add(entity)

		val pos = entity.pos()
		pos?.removeFromTile(entity)

		val trail = entity.trailing()
		if (trail != null)
		{
			for (e in trail.entities.toList())
			{
				processEntity(e)
			}
		}

		if (entity.stats() != null && entity.stats().hp <= 0f && entity.stats().deathEffect != null)
		{
			val effect = entity.stats().deathEffect!!.copy()
			effect.size[0] = pos.size
			effect.size[1] = pos.size
			effect.addToEngine(pos.position)
		}

		val activeAbility = Mappers.activeAbility.get(entity)
		if (activeAbility != null)
		{
			activeAbility.ability.cancel()
		}

		engine.removeEntity(entity)
	}
}
