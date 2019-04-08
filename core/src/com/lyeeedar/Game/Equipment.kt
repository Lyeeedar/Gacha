package com.lyeeedar.Game

import com.lyeeedar.Ascension
import com.lyeeedar.Components.applyAscensionAndLevel
import com.lyeeedar.EquipmentWeight
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.FastEnumMap

class Equipment
{
	val baseStats = FastEnumMap<Statistic, Float>(Statistic::class.java)

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	var equipmentWeight: EquipmentWeight = EquipmentWeight.MEDIUM

	lateinit var icon: Sprite

	fun getStat(statistic: Statistic): Float
	{
		var value = baseStats[statistic] ?: 0f

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

		val rating = hp + power

		return rating
	}
}