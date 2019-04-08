package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.EventType
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.BloodSplatter
import com.lyeeedar.Util.Random

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

		val entityStats = entity.stats()
		if (entityStats != null && entityStats.hp <= 0f && entityStats.deathEffect != null)
		{
			val effect = entityStats.deathEffect!!.copy()
			effect.size[0] = pos.size
			effect.size[1] = pos.size
			effect.addToEngine(pos.position)

			// all the blood
			val numBlood = Random.random(10, 20)
			for (i in 0 until numBlood)
			{
				BloodSplatter.splatter(entityStats.lastHitSource, pos.position, 3f, 90f)
			}

			val killerTile = level!!.getTile(entityStats.lastHitSource)
			if (killerTile != null)
			{
				for (slot in SpaceSlot.EntityValues)
				{
					val e = killerTile.contents[slot] ?: continue
					val stats = e.stats() ?: continue

					if (stats.faction != entityStats.faction)
					{
						// we have our killer!
						val eventData = EventData(EventType.KILL, e, entity, ObjectFloatMap())
						engine.event().addEvent(eventData)
					}
				}
			}

			// send death events
			for (tile in level!!.grid)
			{
				for (slot in SpaceSlot.EntityValues)
				{
					val e = tile.contents[slot] ?: continue
					val stats = e.stats() ?: continue

					val eventData = EventData(EventType.ANYDEATH, e, entity, ObjectFloatMap())
					engine.event().addEvent(eventData)

					if (stats.faction == entityStats.faction)
					{
						val eventData = EventData(EventType.ALLYDEATH, e, entity, ObjectFloatMap())
						engine.event().addEvent(eventData)
					}
					else
					{
						val eventData = EventData(EventType.ENEMYDEATH, e, entity, ObjectFloatMap())
						engine.event().addEvent(eventData)
					}
				}
			}
		}

		val activeAbility = Mappers.activeActionSequence.get(entity)
		if (activeAbility != null)
		{
			activeAbility.sequence.cancel()
		}

		engine.removeEntity(entity)
	}
}
