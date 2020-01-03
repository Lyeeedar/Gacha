package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
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

class DamageAction : AbstractActionSequenceAction()
{
	lateinit var damage: CompiledExpression
	var bonusLifesteal: Float = 0f
	var bonusCritChance: Float = 0f
	var bonusCritDamage: Float = 0f

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

				val targetstats = entity.statsOrNull() ?: continue
				if (entity.isEnemies(sequence.source))
				{
					val sourceStats = sequence.source.stats()

					map.clear()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					var damModifier = damage.evaluate(map)
					damModifier += damModifier * sourceStats.getStat(Statistic.ABILITYPOWER)

					var attackDam = sourceStats.getAttackDam(damModifier, bonusCritChance, bonusCritDamage)

					if (targetstats.checkAegis())
					{
						if (EventSystem.isEventRegistered(EventType.BLOCK, entity))
						{
							val eventData = EventData.obtain().set(EventType.BLOCK, entity, sequence.source, mapOf(Pair("damage", attackDam.first)))
							Global.engine.event().addEvent(eventData)
						}

						attackDam = Pair(0f, attackDam.second)
						targetstats.blockedDamage = true

						targetstats.messagesToShow.add(MessageData.obtain().set("Blocked!", Colour.CYAN, 0.4f))
					}

					val finalDam = targetstats.dealDamage(attackDam.first, attackDam.second)

					sourceStats.abilityDamageDealt += finalDam

					if (sourceStats.summoner != null)
					{
						sourceStats.summoner!!.stats().abilityDamageDealt += finalDam
					}

					BloodSplatter.splatter(sequence.source.tile()!!, entity.tile()!!, 1f)
					targetstats.lastHitSource = sequence.source.tile()!!

					val lifeSteal = sourceStats.getStat(Statistic.LIFESTEAL) + bonusLifesteal
					val stolenLife = finalDam * lifeSteal
					if (stolenLife > 0f)
					{
						sourceStats.heal(stolenLife)
						sourceStats.healing += stolenLife

						if (EventSystem.isEventRegistered(EventType.HEALED, sequence.source))
						{
							val healEventData = EventData.obtain().set(EventType.HEALED, sequence.source, sequence.source, mapOf(Pair("damage", stolenLife)))
							Global.engine.event().addEvent(healEventData)
						}
					}
					else if (stolenLife < 0f)
					{
						sourceStats.dealDamage(stolenLife, false)
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = DamageAction.obtain()
		action.damage = damage
		action.bonusLifesteal = bonusLifesteal
		action.bonusCritChance = bonusCritChance
		action.bonusCritDamage = bonusCritDamage

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val damageStr = xmlData.get("Amount", "1")!!
		val map = ObjectFloatMap<String>()
		StatisticsComponent.writeDefaultVariables(map, "self")
		StatisticsComponent.writeDefaultVariables(map, "target")

		damage = CompiledExpression(damageStr, map)

		bonusLifesteal = xmlData.getFloat("BonusLifesteal", 0f)
		bonusCritChance = xmlData.getFloat("BonusCritChance", 0f)
		bonusCritDamage = xmlData.getFloat("BonusCritDamage", 0f)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DamageAction> = object : Pool<DamageAction>() {
			override fun newObject(): DamageAction
			{
				return DamageAction()
			}

		}

		@JvmStatic fun obtain(): DamageAction
		{
			val obj = DamageAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { DamageAction.pool.free(this); obtained = false } }
}

class HealAction() : AbstractActionSequenceAction()
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

				val targetstats = entity.statsOrNull() ?: continue
				if (entity.isAllies(sequence.source))
				{
					val sourceStats = sequence.source.stats()

					map.clear()
					sourceStats.write(map, "self")
					targetstats.write(map, "target")

					var healModifier = amount.evaluate(map)
					healModifier += healModifier * sourceStats.getStat(Statistic.ABILITYPOWER)

					val power = sourceStats.getStat(Statistic.POWER)
					val healing = power * healModifier
					targetstats.heal(healing)

					sequence.source.stats().healing += healing

					if (sequence.source.stats().summoner != null)
					{
						sequence.source.stats().summoner!!.stats().healing += healing
					}

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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = HealAction.obtain()
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

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<HealAction> = object : Pool<HealAction>() {
			override fun newObject(): HealAction
			{
				return HealAction()
			}

		}

		@JvmStatic fun obtain(): HealAction
		{
			val obj = HealAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { HealAction.pool.free(this); obtained = false } }
}

class StunAction() : AbstractActionSequenceAction()
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

				val targetstats = entity.statsOrNull() ?: continue
				val task = entity.taskOrNull() ?: continue

				if (entity.isEnemies(sequence.source) && Random.random.nextFloat() > targetstats.getStat(Statistic.AEGIS))
				{
					val sourceStats = sequence.source.stats()

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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = StunAction.obtain()
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

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<StunAction> = object : Pool<StunAction>() {
			override fun newObject(): StunAction
			{
				return StunAction()
			}

		}

		@JvmStatic fun obtain(): StunAction
		{
			val obj = StunAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { StunAction.pool.free(this); obtained = false } }
}

class BuffAction() : AbstractActionSequenceAction()
{
	var isDebuff = false
	lateinit var buff: Buff

	class BuffRecord(val stats: StatisticsComponent, val buff: Buff)
	val appliedToEntities = Array<BuffRecord>(false, 4)

	val hitEntities = ObjectSet<Entity>()
	override fun enter(): Boolean
	{
		val sourcestats = sequence.source.stats()

		hitEntities.clear()
		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			for (entity in tile.contents)
			{
				if (hitEntities.contains(entity)) continue
				hitEntities.add(entity)

				val stats = entity.statsOrNull() ?: continue
				if ((isDebuff && entity.isEnemies(sequence.source)) || (!isDebuff && entity.isAllies(sequence.source)))
				{
					val buff = buff.copy()
					if (buff.useParentNameAndIcon)
					{
						buff.name = sequence.creatingName
						buff.icon = sequence.creatingIcon?.copy()
					}
					buff.isBuff = !isDebuff

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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = BuffAction.obtain()
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

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<BuffAction> = object : Pool<BuffAction>() {
			override fun newObject(): BuffAction
			{
				return BuffAction()
			}

		}

		@JvmStatic fun obtain(): BuffAction
		{
			val obj = BuffAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { BuffAction.pool.free(this); obtained = false } }
}

class SummonAction() : AbstractActionSequenceAction()
{
	lateinit var entityPath: String
	var summonEffect: ParticleEffectDescription? = null
	var killOnExit = false

	val summonedEntities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		for (point in sequence.targets)
		{
			var tile = sequence.level.getTile(point) ?: continue

			// find empty file
			val summonEntity = EntityLoader.load(entityPath, Global.resolveInstant)
			summonEntity.stats().level = sequence.source.stats().level
			summonEntity.stats().ascension = sequence.source.stats().ascension
			summonEntity.stats().factionBuffs.addAll(sequence.source.stats().factionBuffs)
			summonEntity.stats().faction = sequence.source.stats().faction
			summonEntity.stats().statModifier = sequence.source.stats().statModifier

			val abPower = sequence.source.stats().getStat(Statistic.ABILITYPOWER)
			val powerBuff = Buff()
			powerBuff.duration = 9999
			powerBuff.statistics[Statistic.MAXHP] = abPower
			powerBuff.statistics[Statistic.POWER] = abPower
			summonEntity.stats().factionBuffs.add(powerBuff)

			summonEntity.stats().resetHP()

			summonEntity.stats().summoner = sequence.source

			if (summonEntity.pos().isValidTile(tile, summonEntity))
			{

			}
			else
			{
				val validTiles = Array<Tile>(4)
				for (dir in Direction.Values)
				{
					val dirtile = sequence.level.getTile(tile, dir) ?: continue
					if (summonEntity.pos().isValidTile(dirtile, summonEntity))
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
			if (!Global.resolveInstant && summonEffect != null)
			{
				val effect = summonEffect!!.getParticleEffect()
				effect.addToEngine(tile)

				delay = effect.blockinglifetime * 0.7f
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
				entity.add(MarkedForDeletionComponent.obtain())
			}
		}
		summonedEntities.clear()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = SummonAction.obtain()
		action.entityPath = entityPath
		action.summonEffect = summonEffect
		action.killOnExit = killOnExit

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		entityPath = xmlData.get("Entity")

		val effectEl = xmlData.getChildByName("Effect")
		if (effectEl != null)
		{
			summonEffect = AssetManager.loadParticleEffect(effectEl)
		}

		killOnExit = xmlData.getBoolean("KillOnExit", false)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SummonAction> = object : Pool<SummonAction>() {
			override fun newObject(): SummonAction
			{
				return SummonAction()
			}

		}

		@JvmStatic fun obtain(): SummonAction
		{
			val obj = SummonAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SummonAction.pool.free(this); obtained = false } }
}

class ReplaceAttackAction() : AbstractActionSequenceAction()
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

				val targetstats = entity.statsOrNull() ?: continue

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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = ReplaceAttackAction.obtain()
		action.attackDefinition = attackDefinition

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val attackDefEl = xmlData.getChildByName("Attack")!!
		attackDefinition = AttackDefinition()
		attackDefinition.parse(attackDefEl)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ReplaceAttackAction> = object : Pool<ReplaceAttackAction>() {
			override fun newObject(): ReplaceAttackAction
			{
				return ReplaceAttackAction()
			}

		}

		@JvmStatic fun obtain(): ReplaceAttackAction
		{
			val obj = ReplaceAttackAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { ReplaceAttackAction.pool.free(this); obtained = false } }
}

class ModifyBuff() : AbstractActionSequenceAction()
{
	var modifyBuff = false
	var name: String? = null
	var amount = 0

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

				val targetstats = entity.statsOrNull() ?: continue

				val targetsEnemies = (!modifyBuff && amount > 0) || (modifyBuff && amount < 0)
				if (
					(targetsEnemies && entity.isEnemies(sequence.source)) ||
					(!targetsEnemies && entity.isAllies(sequence.source)))
				{
					for (buff in targetstats.buffs)
					{
						if (buff.isBuff == modifyBuff && buff.duration != 0)
						{
							if (name == null || buff.name.startsWith(name!!))
							{
								buff.duration += amount
							}
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = ModifyBuff.obtain()
		action.modifyBuff = modifyBuff
		action.name = name
		action.amount = amount

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		modifyBuff = xmlData.name == "ModifyBuff"
		name = xmlData.get("Name", null)
		if (name == "") name = null
		amount = xmlData.getInt("Amount", 0)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ModifyBuff> = object : Pool<ModifyBuff>() {
			override fun newObject(): ModifyBuff
			{
				return ModifyBuff()
			}

		}

		@JvmStatic fun obtain(): ModifyBuff
		{
			val obj = ModifyBuff.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { ModifyBuff.pool.free(this); obtained = false } }
}