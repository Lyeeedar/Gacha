package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.EventAndCondition
import com.lyeeedar.Components.EventHandlerComponent
import com.lyeeedar.EventType
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class Buff
{
	lateinit var description: String

	var duration = 0
	var icon: Sprite? = null
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val eventHandlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	var source: Any? = null

	fun copy(): Buff
	{
		val buff = Buff()
		buff.duration = duration
		buff.icon = icon
		buff.statistics.addAll(statistics)

		return buff
	}

	fun parse(xml: XmlData)
	{
		val iconEl = xml.getChildByName("Icon")
		if (iconEl != null)
		{
			icon = AssetManager.loadSprite(iconEl)
		}

		Statistic.parse(xml.getChildByName("Statistics")!!, statistics)

		val eventHandlersEl = xml.getChildByName("EventHandlers")
		if (eventHandlersEl != null)
		{
			EventHandlerComponent.parseEvents(eventHandlers, eventHandlersEl)
		}

		duration = xml.getInt("Duration", 0)

		description = xml.get("Description", "")!!

		description += "\n[GOLD]"
		var first = true
		for (stat in Statistic.Values)
		{
			val value = statistics[stat] ?: continue
			if (value != 0f)
			{
				if (!first)
				{
					description += ", "
				}
				first = false

				if (value > 0)
				{
					description += "+"
				}

				description += (value * 100).toInt().toString() + "% " + stat.niceName
			}
		}

		description += "[]."
	}

	companion object
	{
		fun load(xml: XmlData): Buff
		{
			val buff = Buff()
			buff.parse(xml)
			return buff
		}
	}
}