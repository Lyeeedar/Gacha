package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.EventAndCondition
import com.lyeeedar.Components.applyAscensionAndLevel
import com.lyeeedar.Renderables.Sprite.MaskedTextureData
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class Equipment
{
	lateinit var name: String
	lateinit var icon: MaskedTextureData

	var weight: EquipmentWeight = EquipmentWeight.MEDIUM
	var slot: EquipmentSlot = EquipmentSlot.WEAPON

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	val stats = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val eventHandlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	fun getStat(statistic: Statistic): Float
	{
		var value = stats[statistic] ?: 0f

		// apply level / rarity, but only to the base stats
		if (Statistic.BaseValues.contains(statistic))
		{
			value = value.applyAscensionAndLevel(level, ascension)
		}

		return value
	}

	fun calculatePowerRating(): Float
	{
		var hp = getStat(Statistic.MAXHP)
		hp *= 1f + getStat(Statistic.AEGIS)
		hp *= 1f + getStat(Statistic.DR)
		hp *= 1f + getStat(Statistic.REGENERATION) * 2f
		hp *= 1f + getStat(Statistic.LIFESTEAL)

		var power = getStat(Statistic.POWER) * 10f
		power += (power * (1f + getStat(Statistic.CRITDAMAGE))) * getStat(Statistic.CRITCHANCE)
		power *= 1f + getStat(Statistic.HASTE)

		var rating = hp + power

		for (handlers in eventHandlers)
		{
			rating *= 1f + 0.2f * handlers.size
		}

		return rating
	}

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name", "")!!
		icon = MaskedTextureData(xmlData.getChildByName("Icon")!!)

		weight = EquipmentWeight.valueOf(xmlData.get("Weight", "Medium")!!.toUpperCase())
		slot = EquipmentSlot.valueOf(xmlData.get("Slot", "Weapon")!!.toUpperCase())

		level = xmlData.getInt("Level", 1)
		ascension = Ascension.valueOf(xmlData.get("Ascension", "Mundane")!!.toUpperCase())

		Statistic.parse(xmlData.getChildByName("Statistics"), stats)
		EventType.parseEvents(xmlData.getChildByName("EventHandlers"), eventHandlers)
	}

	companion object
	{
		fun load(xmlData: XmlData): Equipment
		{
			val equip = Equipment()
			equip.parse(xmlData)

			return equip
		}
	}
}