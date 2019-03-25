package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Util.XmlData

class AbilityComponent : AbstractComponent()
{
	val abilities = Array<AbilityData>()

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val abilitiesEl = xml.getChildByName("Abilities")!!
		for (el in abilitiesEl.children)
		{
			val abilityEl = el.getChildByName("Ability")!!
			val ability = Ability.load(abilityEl)

			val cooldown = com.lyeeedar.Util.IntRange(el.getPoint("Cooldown"))

			val data = AbilityData()
			data.ability = ability
			data.cooldown = cooldown
			data.remainingCooldown = data.cooldown.getValue()

			abilities.add(data)
		}
	}
}

class AbilityData
{
	lateinit var ability: Ability
	lateinit var cooldown: com.lyeeedar.Util.IntRange

	var remainingCooldown = 0
}