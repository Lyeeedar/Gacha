package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*

class StatisticsComponent: AbstractComponent()
{
	var faction: String = ""

	var hp: Float = 0f
		get() = field
		set(value)
		{
			if (value == field) return

			var v = Math.min(value, getStat(Statistic.MAXHEALTH))

			var diff = v - hp
			if (diff < 0)
			{
				if (invulnerable)
				{
					blockedDamage = true
					return
				}
			}

			diff = v - hp

			if (v < field)
			{
				tookDamage = true
			}

			lostHp -= diff
			if (lostHp < 0)
			{
				lostHp = 0f
			}
			maxLostHp = max(maxLostHp, lostHp)

			field = v
			if (godMode && field < 1) field = 1f
		}
	var lostHp = 0f
	var maxLostHp = 0f

	val baseStats = FastEnumMap<Statistic, Float>(Statistic::class.java)

	var deathEffect: ParticleEffect? = null

	var blocking = false
	var invulnerable = false
	var godMode = false

	var tookDamage = false
	var blockedDamage = false
	var blockBroken = false

	lateinit var attackDefinition: AttackDefinition

	val buffs = Array<Buff>(false, 4)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		Statistic.parse(xml.getChildByName("Statistics")!!, baseStats)
		hp = getStat(Statistic.MAXHEALTH)

		val deathEl = xml.getChildByName("Death")
		if (deathEl != null) deathEffect = AssetManager.loadParticleEffect(deathEl)

		val attackEl = xml.getChildByName("Attack")!!
		attackDefinition = AttackDefinition()
		attackDefinition.parse(attackEl)
	}

	fun dealDamage(amount: Float, pierce: Float)
	{
		val baseDam = amount
		val applicableDefense = max(0f, getStat(Statistic.DEFENSE) - pierce)

		val alpha = 1f / clamp(applicableDefense / baseDam, 0.5f, 2f)

		val dam = baseDam * alpha

		hp -= dam
	}

	fun get(key: String): Float
	{
		val key = key.toLowerCase()
		if (key == "hp")
		{
			return hp
		}

		for (stat in Statistic.Values)
		{
			if (stat.toString().toLowerCase() == key)
			{
				return getStat(stat)
			}
		}

		return 0f
	}

	fun getStat(statistic: Statistic): Float
	{
		var value = baseStats[statistic] ?: 0f

		var modifier = 0f
		for (buff in buffs)
		{
			modifier += buff.statistics[statistic] ?: 0f
		}

		if (statistic.modifiersAreAdded)
		{
			value += modifier
		}
		else
		{
			value += modifier * value
		}

		return clamp(value, statistic.min, statistic.max)
	}

	fun getCritMultiplier(): Float
	{
		val critChance = getStat(Statistic.CRITCHANCE)
		if (Random.random() <= critChance)
		{
			val mult = getStat(Statistic.CRITDAMAGE)
			return mult
		}

		return 1f
	}

	fun getAttackDam(multiplier: Float): Float
	{
		val baseAttack = getStat(Statistic.POWER)

		var modifier = Random.random()
		modifier *= modifier
		modifier *= 0.2f // 20% range
		modifier *= Random.sign()

		val attack = baseAttack + baseAttack * modifier

		val withCritChance = attack * getCritMultiplier()

		return withCritChance * multiplier
	}

	fun write(variableMap: ObjectFloatMap<String>, prefixName: String? = null): ObjectFloatMap<String>
	{
		val prefix = if (prefixName != null) "$prefixName." else ""

		variableMap.put(prefix + "hp", hp)

		for (stat in Statistic.Values)
		{
			variableMap.put(prefix + stat.toString().toLowerCase(), getStat(stat))
		}

		return variableMap
	}

	override fun saveData(kryo: Kryo, output: Output)
	{
		output.writeFloat(hp)
	}

	override fun loadData(kryo: Kryo, input: Input)
	{
		hp = input.readFloat()

		tookDamage = false
	}
}

class AttackDefinition
{
	var damage: Float = 1f
	var range: Int = 1
	var hitEffect: ParticleEffect? = null
	var flightEffect: ParticleEffect? = null

	fun parse(xml: XmlData)
	{
		damage = xml.getFloat("Damage", 1f)
		range = xml.getInt("Range", 1)

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null) hitEffect = AssetManager.loadParticleEffect(hitEffectEl)

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null) flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
	}
}

class Buff
{
	lateinit var icon: Sprite
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)

	fun parse(xml: XmlData)
	{
		icon = AssetManager.loadSprite(xml.getChildByName("Icon")!!)
		Statistic.parse(xml.getChildByName("Statistics")!!, statistics)
	}
}