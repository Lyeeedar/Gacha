package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.Components.*
import com.lyeeedar.EventType
import com.lyeeedar.Global
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.ObjectFloatMap
import com.lyeeedar.Util.Point
import ktx.collections.gdxArrayOf

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
					checkHandlers(event, equip.eventHandlers)
				}
			}

			val eventHandler = event.source.eventHandler()
			if (eventHandler != null)
			{
				checkHandlers(event, eventHandler.handlers)
			}
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
					sequence.source = eventData.source
					sequence.targets.clear()
					sequence.targets.addAll(eventData.targets)

					if (eventData.targetEntity != null)
					{
						sequence.lockedTargets.add(eventData.targetEntity!!)
					}

					val entity = Entity()
					entity.add(ActiveActionSequenceComponent(sequence))
					entity.add(TransientComponent())

					Global.engine.addEntity(entity)
				}
			}
		}
	}

	fun addEvent(eventData: EventData)
	{
		queuedEvents.add(eventData)
	}
}

class EventData(val type: EventType, val source: Entity, val targets: Array<Point>, val variables: ObjectFloatMap<String>)
{
	var targetEntity: Entity? = null

	constructor(type: EventType, source: Entity, target: Entity, variables: ObjectFloatMap<String>) : this(type, source, gdxArrayOf(target.pos().position), variables)
	{
		targetEntity = target

		source.stats().write(variables, "self")
		target.stats().write(variables, "target")
	}

	constructor(type: EventType, source: Entity, target: Entity, variables: Map<String, Float>) : this(type, source, gdxArrayOf(target.pos().position), ObjectFloatMap(variables))
}