package com.lyeeedar.Game.Ability

import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.stats
import com.lyeeedar.Components.task
import com.lyeeedar.Util.XmlData

class DamageAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var damage: Float = 0f

	override fun enter(): Boolean
	{
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				val stats = entity.stats() ?: continue
				stats.dealDamage(damage, true)
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = DamageAction(ability)
		action.damage = damage

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		damage = xmlData.getFloat("Damage")
	}
}

class StunAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var chance: CompiledExpression

	override fun enter(): Boolean
	{
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				val task = entity.task() ?: continue

				if (chance.evaluate() != 0f)
				{
					task.tasks.clear()
					task.tasks.add(TaskInterrupt())
				}
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = StunAction(ability)
		action.chance = chance

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val chanceStr = xmlData.get("Chance")
		chance = CompiledExpression(chanceStr)
	}

}