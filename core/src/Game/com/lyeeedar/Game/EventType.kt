package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Components.EventAndCondition
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

// ----------------------------------------------------------------------
enum class EventType
{
	ATTACK,
	MOVE,
	USEABILITY,
	DEALDAMAGE,
	TAKEDAMAGE,
	KILL,
	ALLYDEATH,
	ENEMYDEATH,
	ANYDEATH,
	HEALED,
	CRIT,
	BLOCK,
	ONTURN;

	companion object
	{
		fun parseEvents(xml: XmlData?, holder: FastEnumMap<EventType, Array<EventAndCondition>>)
		{
			if (xml == null) return

			val eventsEl = xml.getChildByName("Events")!!
			for (eventEl in eventsEl.children)
			{
				val type = EventType.valueOf(eventEl.name.toUpperCase())
				val handlers = Array<EventAndCondition>(1)

				for (handlerEl in eventEl.children)
				{
					val rawcondition = handlerEl.get("Condition").toLowerCase()
					val condition = CompiledExpression(rawcondition)

					val sequence = ActionSequence.load(handlerEl.getChildByName("ActionSequence")!!)

					handlers.add(EventAndCondition(condition, sequence))
				}

				holder[type] = handlers
			}
		}
	}
}