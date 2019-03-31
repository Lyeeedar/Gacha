package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
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

			val name = el.get("Name")
			val icon = AssetManager.loadSprite(el.getChildByName("Icon")!!)
			val cooldown = com.lyeeedar.Util.IntRange(el.getPoint("Cooldown", Point(5, 10)))
			val singleUse = el.getBoolean("SingleUse", false)
			val availableOnStart = el.getBoolean("AvailableOnStart", false)
			val conditionStr = el.get("Condition", "1")!!
			val condition = CompiledExpression(conditionStr, StatisticsComponent.getDefaultVariables())
			val range = el.getInt("Range", 1)

			val data = AbilityData()
			data.name = name
			data.icon = icon
			data.ability = ability
			data.cooldown = cooldown
			data.singleUse = singleUse
			data.availableOnStart = availableOnStart
			data.condition = condition
			data.range = range
			data.remainingCooldown = if (availableOnStart) 0f else data.cooldown.getValue().toFloat()

			abilities.add(data)
		}
	}
}

class AbilityData
{
	lateinit var name: String
	lateinit var icon: Sprite

	lateinit var ability: Ability
	lateinit var cooldown: com.lyeeedar.Util.IntRange
	var singleUse = false
	var availableOnStart = false
	lateinit var condition: CompiledExpression
	var range: Int = 1

	var selectedCooldown = 0f
	var remainingCooldown = 0f
	var justUsed = false
}