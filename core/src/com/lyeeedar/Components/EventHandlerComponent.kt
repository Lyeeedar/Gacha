package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.EventType
import com.lyeeedar.EventType.Companion.parseEvents
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class EventAndCondition(val condition: CompiledExpression, val sequence: ActionSequence)

class EventHandlerComponent : AbstractComponent()
{
	val handlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		parseEvents(xml, handlers)
	}
}