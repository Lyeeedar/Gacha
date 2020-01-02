package com.lyeeedar.Game

import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

// ----------------------------------------------------------------------
enum class Statistic private constructor(val min: Float, val max: Float, val modifiersAreAdded: Boolean, val niceName: String, val tooltip: String)
{
	MAXHP(1f, Float.MAX_VALUE, false, "Health", "The amount of damage you can take before dieing."),
	POWER(1f, Float.MAX_VALUE, false, "Power", "The damage of your attacks and the effectiveness of your abilities."),
	CRITCHANCE(0f, 1f, true, "Critical Chance", "The chance to deal a critical hit anytime you deal damage."),
	CRITDAMAGE(1f, Float.MAX_VALUE, true, "Critical Damage", "The multiplier to your damage when you deal a critical hit."),

	DR(-Float.MAX_VALUE, 0.8f, true, "Damage Resistance", "Your resistance to damage."),
	REGENERATION(-1f, 1f, true, "Regeneration", "The percentage of your max health you heal each turn."),
	LIFESTEAL(-Float.MAX_VALUE, Float.MAX_VALUE, true, "Life Steal", "The portion of the damage you deal you absorb as life."),
	AEGIS(0f, 0.8f, true, "Aegis", "The chance to completely block damage when hit."),

	HASTE(-1f, 1f, true, "Haste", "How fast you act."),
	FLEETFOOT(-1f, 1f, true, "Fleet Foot", "How fast you move."),
	DERVISH(-1f, 1f, true, "Dervish", "How fast you attack."),

	ALLYBOOST(-1f, 1f, true, "Ally Boost", "The percentage your power is increased by for each surviving ally."),

	BUFFPOWER(0f, Float.MAX_VALUE, true, "Buff Power", "The bonus to the power of buffs you create."),
	DEBUFFPOWER(0f, Float.MAX_VALUE, true, "Debuff Power", "The bonus to the power of debuffs you create."),
	ABILITYCOOLDOWN(-Float.MAX_VALUE, Float.MAX_VALUE, true, "Ability Cooldown", "The rate at which your abilities come off cooldown."),
	ABILITYPOWER(-1f, Float.MAX_VALUE, true, "Ability Power", "The modifier to your power when used with your abilities."),

	ROOT(0f, 1f, true, "Root", "The chance to fail to move, anytime you move."),
	FUMBLE(0f, 1f, true, "Fumble", "The chance to fail to attack, anytime you attack"),
	DISTRACTION(0f, 1f, true, "Distraction", "The chance to fail to use an ability, anytime you use an ability.");

	companion object
	{
		val Values = Statistic.values()
		val BaseValues = arrayOf(MAXHP, POWER)
		val CoreValues = arrayOf(MAXHP, POWER, DR, CRITCHANCE, CRITDAMAGE)

		fun parse(xmlData: XmlData?, statistics: FastEnumMap<Statistic, Float>)
		{
			if (xmlData == null) return

			for (stat in Values)
			{
				var value = statistics[stat] ?: 0f
				value = xmlData.getFloat(stat.toString(), value)
				statistics[stat] = value
			}
		}
	}
}