package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Ascension
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.EquipmentWeight
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Faction
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class StatisticsComponent: AbstractComponent()
{
	var faction: String = ""
	var factionData: Faction? = null

	var hp: Float = 0f
		get() = field
		private set(value)
		{
			if (value == field) return

			var v = Math.min(value, getStat(Statistic.MAXHP))

			var diff = v - hp
			if (diff < 0)
			{
				if (invulnerable)
				{
					blockedDamage = true
					return
				}
			}

			v = hp + diff

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

			if (hp <= 0)
			{
				lostHp = 0f
				maxLostHp = 0f
			}
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

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	lateinit var attackDefinition: AttackDefinition

	val buffs = Array<Buff>(false, 4)
	val factionBuffs = Array<Buff>(false, 4)

	var lastHitSource = Point()

	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)
	var equipmentWeight: EquipmentWeight = EquipmentWeight.MEDIUM

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		Statistic.parse(xml.getChildByName("Statistics")!!, baseStats)
		hp = getStat(Statistic.MAXHP)

		equipmentWeight = EquipmentWeight.valueOf(xml.get("EquipmentWeight", "Medium")!!.toUpperCase())

		val deathEl = xml.getChildByName("Death")
		if (deathEl != null) deathEffect = AssetManager.loadParticleEffect(deathEl)

		val attackEl = xml.getChildByName("Attack")!!
		attackDefinition = AttackDefinition()
		attackDefinition.parse(attackEl)
	}

	fun resetHP()
	{
		hp = getStat(Statistic.MAXHP)
		lostHp = 0f
		maxLostHp = 0f
		tookDamage = false
	}

	fun dealDamage(amount: Float): Float
	{
		val baseDam = amount
		val dr = getStat(Statistic.DR)

		var dam = baseDam - dr * baseDam

		val aegisChance = getStat(Statistic.AEGIS)
		if (aegisChance > 0f && Random.random() < aegisChance)
		{
			dam = 0f
			blockedDamage = true
		}

		hp -= dam

		val maxHP = getStat(Statistic.MAXHP)
		val alpha = dam / maxHP
		val size = lerp(0.25f, 1f, clamp(alpha, 0f, 1f))

		messagesToShow.add(MessageData(dam.toInt().toString(), Colour.RED, size))

		return dam
	}

	fun heal(amount: Float)
	{
		hp += amount

		val maxHP = getStat(Statistic.MAXHP)
		val alpha = amount / maxHP
		val size = lerp(0.25f, 1f, clamp(alpha, 0f, 1f))
		messagesToShow.add(MessageData(amount.toInt().toString(), Colour.GREEN, size))
	}

	fun regenerate(amount: Float)
	{
		hp += amount
	}

	val messagesToShow = Array<MessageData>()

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

		// apply level / rarity, but only to the base stats
		if (Statistic.BaseValues.contains(statistic))
		{
			value = value.applyAscensionAndLevel(level, ascension)
		}

		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot] ?: continue
			value += equip.getStat(statistic)
		}

		// apply buffs and equipment
		var modifier = 0f
		for (buff in buffs)
		{
			modifier += buff.statistics[statistic] ?: 0f
		}

		for (buff in factionBuffs)
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

		val critAttack = attack * getCritMultiplier()

		return critAttack * multiplier
	}

	fun calculatePowerRating(entity: Entity): Float
	{
		val oldFactionBuffs = factionBuffs.toGdxArray()
		val oldBuffs = buffs.toGdxArray()

		factionBuffs.clear()
		buffs.clear()

		var hp = getStat(Statistic.MAXHP)
		hp *= 1f + getStat(Statistic.AEGIS)
		hp *= 1f + getStat(Statistic.DR)
		hp *= 1f + getStat(Statistic.REGENERATION) * 2f
		hp *= 1f + getStat(Statistic.LIFESTEAL)

		var power = getStat(Statistic.POWER) * 10f
		power += (power * (1f + getStat(Statistic.CRITDAMAGE))) * getStat(Statistic.CRITCHANCE)
		power *= 1f + getStat(Statistic.HASTE)

		var rating = hp + power
		val ability = entity.ability()
		if (ability != null)
		{
			var abilityModifier = 1f + (ability.abilities.size * 0.2f)
			abilityModifier *= 1f + getStat(Statistic.BUFFPOWER)
			abilityModifier *= 1f + getStat(Statistic.BUFFDURATION)
			abilityModifier *= 1f + getStat(Statistic.DEBUFFPOWER)
			abilityModifier *= 1f + getStat(Statistic.DEBUFFDURATION)
			abilityModifier *= 1f + getStat(Statistic.ABILITYPOWER)
			abilityModifier *= 1f + getStat(Statistic.ABILITYCOOLDOWN)

			rating *= abilityModifier
		}

		factionBuffs.addAll(oldFactionBuffs)
		buffs.addAll(oldBuffs)

		return rating
	}

	fun variables(): ObjectFloatMap<String>
	{
		val map = ObjectFloatMap<String>()
		write(map)
		return map
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

	companion object
	{
		fun getDefaultVariables(): ObjectFloatMap<String>
		{
			val map = ObjectFloatMap<String>()
			writeDefaultVariables(map)

			return map
		}

		fun writeDefaultVariables(variableMap: ObjectFloatMap<String>, prefixName: String? = null): ObjectFloatMap<String>
		{
			val prefix = if (prefixName != null) "$prefixName." else ""

			variableMap.put(prefix + "hp", 0f)

			for (stat in Statistic.Values)
			{
				variableMap.put(prefix + stat.toString().toLowerCase(), 0f)
			}

			return variableMap
		}
	}
}

class MessageData(val text: String, val colour: Colour, val size: Float)

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

fun Float.applyAscensionAndLevel(level: Int, ascension: Ascension): Float
{
	var value = this
	value *= Math.pow(1.05, level.toDouble()).toFloat() // 5% per level
	value *= ascension.multiplier

	return value
}