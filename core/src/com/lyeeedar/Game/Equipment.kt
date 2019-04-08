package com.lyeeedar.Game

import com.lyeeedar.Ascension
import com.lyeeedar.Components.applyAscensionAndLevel
import com.lyeeedar.EquipmentWeight
import com.lyeeedar.Statistic
import com.lyeeedar.Util.FastEnumMap

class Equipment
{
	val baseStats = FastEnumMap<Statistic, Float>(Statistic::class.java)

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	var equipmentWeight: EquipmentWeight = EquipmentWeight.MEDIUM

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
}