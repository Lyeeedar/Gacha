package com.lyeeedar.Game.Ability

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Statistic
import com.lyeeedar.Util.XmlData

class DamageAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var damage: Float = 1f

	override fun enter(): Boolean
	{
		val hitEntities = ObjectSet<Entity>()
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val stats = entity.stats() ?: continue
				if (entity.isEnemies(ability.source))
				{
					val sourceStats = ability.source.stats()!!

					val attackDam = sourceStats.getAttackDam(damage)
					stats.dealDamage(attackDam, sourceStats.getStat(Statistic.PIERCE))
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
		val action = DamageAction(ability)
		action.damage = damage

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		damage = xmlData.getFloat("Amount", 1f)
	}
}

class HealAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var amount: Float = 1f

	override fun enter(): Boolean
	{
		val hitEntities = ObjectSet<Entity>()
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val stats = entity.stats() ?: continue
				if (entity.isAllies(ability.source))
				{
					val sourceStats = ability.source.stats()!!

					val power = sourceStats.getStat(Statistic.POWER)
					stats.hp += power * amount
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
		val action = HealAction(ability)
		action.amount = amount

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		amount = xmlData.getFloat("Amount", 1f)
	}
}

class StunAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var chance: CompiledExpression

	override fun enter(): Boolean
	{
		val hitEntities = ObjectSet<Entity>()
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val task = entity.task() ?: continue

				if (entity.isEnemies(ability.source) && chance.evaluate() != 0f)
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

class BuffAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var isDebuff = false
	lateinit var buff: Buff

	val appliedToEntities = Array<StatisticsComponent>(false, 4)

	override fun enter(): Boolean
	{
		val hitEntities = ObjectSet<Entity>()
		for (point in ability.targets)
		{
			val tile = ability.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val stats = entity.stats() ?: continue
				if ((isDebuff && entity.isEnemies(ability.source)) || entity.isAllies(ability.source))
				{
					stats.buffs.add(buff)
					appliedToEntities.add(stats)
				}
			}
		}

		return false
	}

	override fun exit()
	{
		for (stats in appliedToEntities)
		{
			stats.buffs.removeValue(buff, true)
		}
		appliedToEntities.clear()
	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = BuffAction(ability)
		action.isDebuff = isDebuff
		action.buff = buff

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		isDebuff = xmlData.name.toLowerCase() == "debuff"
		buff = Buff()
		buff.parse(xmlData.getChildByName("Buff")!!)
	}
}