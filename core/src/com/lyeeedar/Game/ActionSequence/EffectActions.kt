package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.EventType
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.EventData
import com.lyeeedar.Systems.EventSystem
import com.lyeeedar.Systems.event
import com.lyeeedar.Util.*

class DamageAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	lateinit var damage: CompiledExpression

	val hitEntities = ObjectSet<Entity>()
	val map = ObjectFloatMap<String>()
	override fun enter(): Boolean
	{
		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val targetstats = entity.stats() ?: continue
				if (entity.isEnemies(sequence.source))
				{
					val sourceStats = sequence.source.stats()!!

					map.clear()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					var damModifier = damage.evaluate(map)
					damModifier += damModifier * sourceStats.getStat(Statistic.ABILITYPOWER)

					val attackDam = sourceStats.getAttackDam(damModifier)
					val finalDam = targetstats.dealDamage(attackDam.first)

					sourceStats.damageDealt += finalDam

					BloodSplatter.splatter(sequence.source.tile()!!, entity.tile()!!, 1f)
					targetstats.lastHitSource = sequence.source.tile()!!

					val lifeSteal = sourceStats.getStat(Statistic.LIFESTEAL)
					val stolenLife = finalDam * lifeSteal
					if (stolenLife > 0f)
					{
						sourceStats.heal(stolenLife)

						if (EventSystem.isEventRegistered(EventType.HEALED, sequence.source))
						{
							val healEventData = EventData.obtain().set(EventType.HEALED, sequence.source, sequence.source, mapOf(Pair("damage", stolenLife)))
							Global.engine.event().addEvent(healEventData)
						}
					}

					// do damage events

					// crit
					if (attackDam.second)
					{
						if (EventSystem.isEventRegistered(EventType.CRIT, sequence.source))
						{
							val dealEventData = EventData.obtain().set(EventType.CRIT, sequence.source, entity, mapOf(
								Pair("damage", finalDam),
								Pair("dist", sequence.source.pos().position.dist(entity.pos().position).toFloat())))
							Global.engine.event().addEvent(dealEventData)
						}
					}

					// deal damage
					if (EventSystem.isEventRegistered(EventType.DEALDAMAGE, sequence.source))
					{
						val dealEventData = EventData.obtain().set(EventType.DEALDAMAGE, sequence.source, entity, mapOf(
							Pair("damage", finalDam),
							Pair("dist", sequence.source.pos().position.dist(entity.pos().position).toFloat())))
						Global.engine.event().addEvent(dealEventData)
					}

					// take damage
					if (EventSystem.isEventRegistered(EventType.TAKEDAMAGE, entity))
					{
						val takeEventData = EventData.obtain().set(EventType.TAKEDAMAGE, entity, sequence.source, mapOf(
							Pair("damage", finalDam),
							Pair("dist", sequence.source.pos().position.dist(entity.pos().position).toFloat())))
						Global.engine.event().addEvent(takeEventData)
					}
				}
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = DamageAction(sequence)
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

class HealAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	lateinit var amount: CompiledExpression

	val hitEntities = ObjectSet<Entity>()
	val map = ObjectFloatMap<String>()
	override fun enter(): Boolean
	{
		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val targetstats = entity.stats() ?: continue
				if (entity.isAllies(sequence.source))
				{
					val sourceStats = sequence.source.stats()!!

					map.clear()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					var healModifier = amount.evaluate(map)
					healModifier += healModifier * sourceStats.getStat(Statistic.ABILITYPOWER)

					val power = sourceStats.getStat(Statistic.POWER)
					val healing = power * healModifier
					targetstats.heal(healing)

					if (EventSystem.isEventRegistered(EventType.HEALED, entity))
					{
						val healEventData = EventData.obtain().set(EventType.HEALED, entity, sequence.source, mapOf(Pair("amount", healing)))
						Global.engine.event().addEvent(healEventData)
					}
				}
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = HealAction(sequence)
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

class StunAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	lateinit var chance: CompiledExpression
	lateinit var count: CompiledExpression

	val hitEntities = ObjectSet<Entity>()
	val map = ObjectFloatMap<String>()
	override fun enter(): Boolean
	{
		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val targetstats = entity.stats() ?: continue
				val task = entity.task() ?: continue

				if (entity.isEnemies(sequence.source) && Random.random.nextFloat() > targetstats.getStat(Statistic.AEGIS))
				{
					val sourceStats = sequence.source.stats()!!

					map.clear()
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

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = StunAction(sequence)
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

class BuffAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	var isDebuff = false
	lateinit var buff: Buff

	class BuffRecord(val stats: StatisticsComponent, val buff: Buff)
	val appliedToEntities = Array<BuffRecord>(false, 4)

	val hitEntities = ObjectSet<Entity>()
	override fun enter(): Boolean
	{
		val sourcestats = sequence.source.stats()!!

		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val stats = entity.stats() ?: continue
				if ((isDebuff && entity.isEnemies(sequence.source)) || entity.isAllies(sequence.source))
				{
					val buff = buff.copy()

					if (isDebuff)
					{
						buff.duration = buff.duration + (buff.duration * sourcestats.getStat(Statistic.DEBUFFPOWER)).ciel()

						for (stat in Statistic.Values)
						{
							val value = buff.statistics[stat] ?: continue
							buff.statistics[stat] = value + value * sourcestats.getStat(Statistic.DEBUFFPOWER)
						}
					}
					else
					{
						buff.duration = buff.duration + (buff.duration * sourcestats.getStat(Statistic.BUFFPOWER)).ciel()

						for (stat in Statistic.Values)
						{
							val value = buff.statistics[stat] ?: continue
							buff.statistics[stat] = value + value * sourcestats.getStat(Statistic.BUFFPOWER)
						}
					}

					buff.source = this

					stats.buffs.add(buff)
					appliedToEntities.add(BuffRecord(stats, buff))
				}
			}
		}

		return false
	}

	override fun exit()
	{
		if (buff.duration == 0)
		{
			for (record in appliedToEntities)
			{
				record.stats.buffs.removeValue(record.buff, true)
			}
		}

		appliedToEntities.clear()
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = BuffAction(sequence)
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

class SummonAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	lateinit var entityPath: String
	lateinit var summonEffect: ParticleEffectDescription
	var killOnExit = false

	val summonedEntities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		for (point in sequence.targets)
		{
			var tile = sequence.level.getTile(point) ?: continue

			// find empty file
			val summonEntity = EntityLoader.load(entityPath)
			summonEntity.stats().level = sequence.source.stats().level
			summonEntity.stats().ascension = sequence.source.stats().ascension
			summonEntity.stats().factionBuffs.addAll(sequence.source.stats().factionBuffs)
			summonEntity.stats().faction = sequence.source.stats().faction

			val abPower = sequence.source.stats().getStat(Statistic.ABILITYPOWER)
			val powerBuff = Buff()
			powerBuff.duration = 9999
			powerBuff.statistics[Statistic.MAXHP] = abPower
			powerBuff.statistics[Statistic.POWER] = abPower
			summonEntity.stats().factionBuffs.add(powerBuff)

			summonEntity.stats().resetHP()

			if (summonEntity.pos()!!.isValidTile(tile, summonEntity))
			{

			}
			else
			{
				val validTiles = Array<Tile>(4)
				for (dir in Direction.Values)
				{
					val dirtile = sequence.level.getTile(tile, dir) ?: continue
					if (summonEntity.pos()!!.isValidTile(dirtile, summonEntity))
					{
						validTiles.add(dirtile)
					}
				}

				if (validTiles.size == 0)
				{
					continue
				}

				tile = validTiles.random()
			}

			var delay = 0f
			if (!Global.resolveInstant)
			{
				val effect = summonEffect.getParticleEffect()
				effect.addToEngine(tile)

				delay = effect.lifetime * 0.3f
			}

			Future.call({
				summonEntity.pos().tile = tile
				summonEntity.pos().addToTile(summonEntity)
				Global.engine.addEntity(summonEntity)
						}, delay)

			summonedEntities.add(summonEntity)
		}

		return false
	}

	override fun exit()
	{
		if (killOnExit)
		{
			for (entity in summonedEntities)
			{
				entity.add(MarkedForDeletionComponent())
			}
		}
		summonedEntities.clear()
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = SummonAction(sequence)
		action.entityPath = entityPath
		action.summonEffect = summonEffect
		action.killOnExit = killOnExit

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		entityPath = xmlData.get("Entity")
		summonEffect = AssetManager.loadParticleEffect(xmlData.getChildByName("Effect")!!)
		killOnExit = xmlData.getBoolean("KillOnExit", false)
	}

}

class ReplaceAttackAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	val replacedAttacks = Array<Pair<Entity, AttackDefinition>>()
	lateinit var attackDefinition: AttackDefinition

	val hitEntities = ObjectSet<Entity>()
	override fun enter(): Boolean
	{
		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val targetstats = entity.stats() ?: continue

				replacedAttacks.add(Pair(entity, targetstats.attackDefinition))
				targetstats.attackDefinition = attackDefinition
			}
		}

		return false
	}

	override fun exit()
	{
		for (entity in replacedAttacks)
		{
			entity.first.stats().attackDefinition = entity.second
		}
		replacedAttacks.clear()
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = ReplaceAttackAction(sequence)
		action.attackDefinition = attackDefinition

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val attackDefEl = xmlData.getChildByName("Attack")!!
		attackDefinition = AttackDefinition()
		attackDefinition.parse(attackDefEl)
	}
}