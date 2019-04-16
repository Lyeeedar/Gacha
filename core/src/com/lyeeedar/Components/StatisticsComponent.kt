package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.Pool
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.*
import com.lyeeedar.Game.ActionSequence.BuffAction
import com.lyeeedar.Game.ActionSequence.DamageAction
import com.lyeeedar.Game.ActionSequence.HealAction
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Faction
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
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

	var deathEffect: ParticleEffectDescription? = null

	var blocking = false
	var invulnerable = false
	var godMode = false

	var tookDamage = false
	var blockedDamage = false
	var blockBroken = false

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	var survivingAllies = 0

	lateinit var attackDefinition: AttackDefinition

	val buffs = Array<Buff>(false, 4)
	val factionBuffs = Array<Buff>(false, 4)

	var lastHitSource = Point()

	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)
	var equipmentWeight: EquipmentWeight = EquipmentWeight.MEDIUM

	var attackDamageDealt = 0f
	var abilityDamageDealt = 0f

	val damageDealt: Float
		get() = attackDamageDealt + abilityDamageDealt

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		Statistic.parse(xml.getChildByName("Statistics")!!, baseStats)
		hp = getStat(Statistic.MAXHP)

		equipmentWeight = EquipmentWeight.valueOf(xml.get("EquipmentWeight", "Medium")!!.toUpperCase())

		val equipmentEl = xml.getChildByName("Equipment")
		if (equipmentEl != null)
		{
			for (el in equipmentEl.children)
			{
				val slot = EquipmentSlot.valueOf(el.name.toUpperCase())
				val equipment = Equipment.load(el)

				this.equipment[slot] = equipment
			}
		}

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

		if (!Global.resolveInstant)
		{
			val maxHP = getStat(Statistic.MAXHP)
			val alpha = dam / maxHP
			val size = lerp(0.25f, 1f, clamp(alpha, 0f, 1f))

			messagesToShow.add(MessageData.obtain().set(dam.ciel().toString(), Colour.RED, size))
		}

		return dam
	}

	fun heal(amount: Float)
	{
		hp += amount

		if (!Global.resolveInstant)
		{
			val maxHP = getStat(Statistic.MAXHP)
			val alpha = amount / maxHP
			val size = lerp(0.25f, 1f, clamp(alpha, 0f, 1f))
			messagesToShow.add(MessageData.obtain().set(amount.ciel().toString(), Colour.GREEN, size))
		}
	}

	fun regenerate(amount: Float)
	{
		hp += amount
	}

	val messagesToShow = Array<MessageData>(1)

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
		else
		{
			value *= 1f + level.toFloat() / 100f + ascension.multiplier / 10f
		}

		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot] ?: continue
			value += equip.getStat(statistic, level)
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

		if (statistic == Statistic.POWER)
		{
			val allyBoost = getStat(Statistic.ALLYBOOST)
			modifier += allyBoost * survivingAllies
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

	fun getCritMultiplier(): Pair<Float, Boolean>
	{
		val critChance = getStat(Statistic.CRITCHANCE)
		if (Random.random() <= critChance)
		{
			val mult = getStat(Statistic.CRITDAMAGE)
			return Pair(mult, true)
		}

		return Pair(1f, false)
	}

	fun getAttackDam(multiplier: Float): Pair<Float, Boolean>
	{
		val baseAttack = getStat(Statistic.POWER)

		var modifier = Random.random()
		modifier *= modifier
		modifier *= 0.2f // 20% range
		modifier *= Random.sign()

		val attack = baseAttack + baseAttack * modifier

		val critMult = getCritMultiplier()

		return Pair(attack * critMult.first * multiplier, critMult.second)
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

		var power = getStat(Statistic.POWER) * 10f
		power += (power * (1f + getStat(Statistic.CRITDAMAGE))) * getStat(Statistic.CRITCHANCE)
		power *= 1f + getStat(Statistic.HASTE)
		power *= 1f + getStat(Statistic.LIFESTEAL)

		var rating = hp + power
		val ability = entity.ability()
		if (ability != null)
		{
			var abilityModifier = (ability.abilities.size * 0.2f)

			for (ability in ability.abilities)
			{
				if (ability.ability.actions.any{ it is DamageAction || it is HealAction })
				{
					abilityModifier += getStat(Statistic.ABILITYPOWER)
				}

				if (ability.ability.actions.any{ it is BuffAction && !it.isDebuff })
				{
					abilityModifier += getStat(Statistic.BUFFPOWER)
				}

				if (ability.ability.actions.any{ it is BuffAction && it.isDebuff })
				{
					abilityModifier += getStat(Statistic.DEBUFFPOWER)
				}

				abilityModifier += getStat(Statistic.ABILITYCOOLDOWN)
			}

			rating *= 1f + abilityModifier * 0.5f
		}

		factionBuffs.addAll(oldFactionBuffs)
		buffs.addAll(oldBuffs)

		return rating
	}

	val map = ObjectFloatMap<String>()
	fun variables(): ObjectFloatMap<String>
	{
		map.clear()
		write(map)
		return map
	}

	fun write(variableMap: ObjectFloatMap<String>, prefixName: String? = null): ObjectFloatMap<String>
	{
		val prefix = if (prefixName != null) "$prefixName." else ""

		variableMap.put(prefix + "hp", hp)
		variableMap.put(prefix + "level", level.toFloat())
		variableMap.put(prefix + "ascension", ascension.ordinal.toFloat())
		variableMap.put(prefix + "ascensionMultiplier", ascension.multiplier)

		for (stat in Statistic.Values)
		{
			variableMap.put(prefix + stat.toString().toLowerCase(), getStat(stat))
		}

		for (buff in buffs)
		{
			variableMap.put(prefix + buff.name, 1f)
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
		val map = ObjectFloatMap<String>()
		init
		{
			writeDefaultVariables(map)
		}

		fun getDefaultVariables(): ObjectFloatMap<String>
		{
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

		fun createSample(): StatisticsComponent
		{
			val comp = StatisticsComponent()
			for (stat in Statistic.Values)
			{
				if (stat == Statistic.MAXHP)
				{
					comp.baseStats[stat] = 100f
				}
				else if (stat == Statistic.POWER)
				{
					comp.baseStats[stat] = 10f
				}
				else
				{
					comp.baseStats[stat] = 0.1f
				}
			}

			return comp
		}
	}
}

class MessageData()
{
	lateinit var text: String
	lateinit var colour: Colour
	var size: Float = 0f

	fun set(text: String, colour: Colour, size: Float): MessageData
	{
		this.text = text
		this.colour = colour
		this.size = size

		return this
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<MessageData> = object : Pool<MessageData>() {
			override fun newObject(): MessageData
			{
				return MessageData()
			}
		}

		@JvmStatic fun obtain(): MessageData
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}

class AttackDefinition
{
	var damage: Float = 1f
	var range: Int = 1
	var hitEffect: ParticleEffectDescription? = null
	var flightEffect: ParticleEffectDescription? = null

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