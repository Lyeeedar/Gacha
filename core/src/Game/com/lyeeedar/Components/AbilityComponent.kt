package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

class AbilityComponent : AbstractComponent()
{
	val abilities = Array<AbilityData>(1)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val abilitiesEl = xml.getChildByName("Abilities")!!
		for (el in abilitiesEl.children)
		{
			val abilityEl = el.getChildByName("Ability")!!
			val ability = ActionSequence.load(abilityEl)

			val name = el.get("Name")
			val description = el.get("Description", "")!!
			val icon = AssetManager.loadSprite(el.getChildByName("Icon")!!)
			val cooldown = com.lyeeedar.Util.IntRange(el.getPoint("Cooldown", Point(5, 10)))
			val singleUse = el.getBoolean("SingleUse", false)
			val availableOnStart = el.getBoolean("AvailableOnStart", false)
			val conditionStr = el.get("Condition", "1")!!.toLowerCase()
			val condition = CompiledExpression(conditionStr, StatisticsComponent.getDefaultVariables())
			val range = el.getInt("Range", 1)
			val cancellable = el.getBoolean("Cancellable", true)
			val removeOnDeath = el.getBoolean("RemoveOnDeath", false)

			val data = AbilityData()
			data.name = name
			data.description = description
			data.icon = icon
			data.ability = ability
			data.cooldown = cooldown
			data.singleUse = singleUse
			data.availableOnStart = availableOnStart
			data.condition = condition
			data.range = range
			data.remainingCooldown = if (availableOnStart) 0f else data.cooldown.getValue().toFloat()
			data.selectedCooldown = data.remainingCooldown
			data.cancellable = cancellable
			data.removeOnDeath = removeOnDeath

			abilities.add(data)
		}
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<AbilityComponent> = object : Pool<AbilityComponent>() {
			override fun newObject(): AbilityComponent
			{
				return AbilityComponent()
			}

		}

		@JvmStatic fun obtain(): AbilityComponent
		{
			val obj = AbilityComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { AbilityComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		abilities.clear()
	}
}

class AbilityData
{
	lateinit var name: String
	lateinit var description: String
	lateinit var icon: Sprite

	lateinit var ability: ActionSequence
	lateinit var cooldown: com.lyeeedar.Util.IntRange
	var singleUse = false
	var availableOnStart = false
	lateinit var condition: CompiledExpression
	var range: Int = 1
	var cancellable = true
	var removeOnDeath = false

	var selectedCooldown = 0f
	var remainingCooldown = 0f
	var justUsed = false
}