package com.lyeeedar.Game.Ability

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Statistic
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.round

class DamageAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var damage: CompiledExpression

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

				val targetstats = entity.stats() ?: continue
				if (entity.isEnemies(ability.source))
				{
					val sourceStats = ability.source.stats()!!

					val map = ObjectFloatMap<String>()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					val damModifier = damage.evaluate(map)

					val attackDam = sourceStats.getAttackDam(damModifier)
					val finalDam = targetstats.dealDamage(attackDam)

					val lifeSteal = sourceStats.getStat(Statistic.LIFESTEAL)
					val stolenLife = finalDam * lifeSteal
					if (stolenLife > 0f)
					{
						sourceStats.heal(stolenLife)
					}
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
		val damageStr = xmlData.get("Amount", "1")!!
		val map = ObjectFloatMap<String>()
		StatisticsComponent.writeDefaultVariables(map, "self")
		StatisticsComponent.writeDefaultVariables(map, "target")

		damage = CompiledExpression(damageStr, map)
	}
}

class HealAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var amount: CompiledExpression

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

				val targetstats = entity.stats() ?: continue
				if (entity.isAllies(ability.source))
				{
					val sourceStats = ability.source.stats()!!

					val map = ObjectFloatMap<String>()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					val healModifier = amount.evaluate(map)

					val power = sourceStats.getStat(Statistic.POWER)
					targetstats.heal(power * healModifier)
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
		val healStr = xmlData.get("Amount", "1")!!

		val map = ObjectFloatMap<String>()
		StatisticsComponent.writeDefaultVariables(map, "self")
		StatisticsComponent.writeDefaultVariables(map, "target")

		amount = CompiledExpression(healStr, map)
	}
}

class StunAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var chance: CompiledExpression
	lateinit var count: CompiledExpression

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

				val targetstats = entity.stats() ?: continue
				val task = entity.task() ?: continue

				if (entity.isEnemies(ability.source) && chance.evaluate() != 0f)
				{
					val sourceStats = ability.source.stats()!!

					val map = ObjectFloatMap<String>()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					val count = count.evaluate(map).round()

					var finalCount = 0
					for (i in 0 until count)
					{
						if (chance.evaluate(map) != 0f)
						{
							finalCount++
						}
					}

					if (finalCount > 0)
					{
						task.tasks.clear()
						for (i in 0 until finalCount)
						{
							task.tasks.add(TaskInterrupt())
						}
					}
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
		action.count = count

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val map = ObjectFloatMap<String>()
		StatisticsComponent.writeDefaultVariables(map, "self")
		StatisticsComponent.writeDefaultVariables(map, "target")

		val chanceStr = xmlData.get("Chance", "1")!!
		chance = CompiledExpression(chanceStr, map)

		val countStr = xmlData.get("Count", "1")!!
		count = CompiledExpression(countStr, map)
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
					val buff = if (buff.duration > 0) buff.copy() else buff

					buff.source = this

					stats.buffs.add(buff)
					appliedToEntities.add(stats)
				}
			}
		}

		return false
	}

	override fun exit()
	{
		if (buff.duration == 0)
		{
			for (stats in appliedToEntities)
			{
				stats.buffs.removeValue(buff, true)
			}
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