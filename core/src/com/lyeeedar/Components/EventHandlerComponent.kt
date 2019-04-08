package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.EventType
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class EventAndCondition(val condition: CompiledExpression, val sequence: ActionSequence)

class EventHandlerComponent : AbstractComponent()
{
	val handlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		parseEvents(handlers, xml)
	}

	companion object
	{
		fun parseEvents(holder: FastEnumMap<EventType, Array<EventAndCondition>>, xml: XmlData)
		{
			val eventsEl = xml.getChildByName("Events")!!
			for (eventEl in eventsEl.children)
			{
				val type = EventType.valueOf(eventEl.name.toUpperCase())
				val handlers = Array<EventAndCondition>()

				for (handlerEl in eventEl.children)
				{
					val rawcondition = handlerEl.get("Condition")
					val condition = CompiledExpression(rawcondition)

					val sequence = ActionSequence.load(handlerEl.getChildByName("ActionSequence")!!)

					handlers.add(EventAndCondition(condition, sequence))
				}

				holder[type] = handlers
			}
		}
	}
}