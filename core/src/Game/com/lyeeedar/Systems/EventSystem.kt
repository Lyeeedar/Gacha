package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.*
import com.lyeeedar.EventType
import com.lyeeedar.Global
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.set

class EventSystem : AbstractSystem()
{
	private val queuedEvents = Array<EventData>()

	private val executingArray = Array<EventData>()
	override fun doUpdate(deltaTime: Float)
	{
		executingArray.clear()
		executingArray.addAll(queuedEvents)
		queuedEvents.clear()

		for (event in executingArray)
		{
			if (event.source.isScheduledForRemoval || event.source.isMarkedForDeletion())
			{
				event.free()
				continue
			}

			val stats = event.source.stats()
			if (stats != null)
			{
				for (buff in stats.buffs)
				{
					checkHandlers(event, buff.eventHandlers)
				}

				for (buff in stats.factionBuffs)
				{
					checkHandlers(event, buff.eventHandlers)
				}

				for (equip in stats.equipment)
				{
					for (provider in equip.statsProviders)
					{
						checkHandlers(event, provider.eventHandlers)
					}
				}
			}

			val eventHandler = event.source.eventHandler()
			if (eventHandler != null)
			{
				checkHandlers(event, eventHandler.handlers)
			}

			event.free()
		}
	}

	private fun checkHandlers(eventData: EventData, eventHandler: FastEnumMap<EventType, Array<EventAndCondition>>)
	{
		val handlers = eventHandler[eventData.type]

		if (handlers != null)
		{
			for (handler in handlers)
			{
				if (handler.condition.evaluate(eventData.variables) != 0f)
				{
					val sequence = handler.sequence.copy()

					sequence.begin(eventData.source,level!!)
					sequence.targets.clear()
					sequence.targets.addAll(eventData.targets)

					if (eventData.targetEntity != null)
					{
						sequence.lockedTargets.add(eventData.targetEntity!!)
					}

					val entity = EntityPool.obtain()
					entity.add(ActiveActionSequenceComponent.obtain().set(sequence))
					entity.add(TransientComponent.obtain())

					Global.engine.addEntity(entity)
				}
			}
		}
	}

	override fun onTurn()
	{
		for (entity in engine.entities)
		{
			if (isEventRegistered(EventType.ONTURN, entity))
			{
				addEvent(EventData.obtain().set(EventType.ONTURN, entity, entity))
			}
		}
	}

	fun addEvent(eventData: EventData)
	{
		queuedEvents.add(eventData)
	}

	companion object
	{
		fun isEventRegistered(type: EventType, entity: Entity): Boolean
		{
			val stats = entity.statsOrNull()
			if (stats != null)
			{
				for (buff in stats.buffs)
				{
					if ((buff.eventHandlers[type]?.size ?: 0) > 0)
					{
						return true
					}
				}

				for (buff in stats.factionBuffs)
				{
					if ((buff.eventHandlers[type]?.size ?: 0) > 0)
					{
						return true
					}
				}

				for (equip in stats.equipment)
				{
					for (provider in equip.statsProviders)
					{
						if ((provider.eventHandlers[type]?.size ?: 0) > 0)
						{
							return true
						}
					}
				}
			}

			val eventHandler = entity.eventHandler()
			if (eventHandler != null)
			{
				if ((eventHandler.handlers[type]?.size ?: 0) > 0)
				{
					return true
				}
			}

			return false
		}
	}
}

class EventData()
{
	lateinit var type: EventType
	lateinit var source: Entity
	val targets = Array<Point>(1)
	val variables = ObjectFloatMap<String>()

	var targetEntity: Entity? = null

	fun set(type: EventType, source: Entity, target: Entity, inputVariables: Map<String, Float>? = null): EventData
	{
		this.type = type
		this.source = source
		this.targets.add(target.pos().position)
		targetEntity = target

		if (inputVariables != null)
		{
			for (pair in inputVariables)
			{
				variables[pair.key] = pair.value
			}
		}

		source.stats().write(variables, "self")
		target.stats().write(variables, "target")

		return this
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<EventData> = object : Pool<EventData>() {
			override fun newObject(): EventData
			{
				return EventData()
			}
		}

		@JvmStatic fun obtain(): EventData
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}