package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Range
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.max

class StatisticsComponent: AbstractComponent()
{
	var faction: String = ""

	var unblockableDam: Boolean = false

	var hp: Float = 0f
		get() = field
		set(value)
		{
			var v = Math.min(value, maxHP)

			var diff = v - hp
			if (diff < 0)
			{
				if (unblockableDam)
				{
					unblockableDam = false
				}
				else if (invulnerable)
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

	var maxHP: Float = 0f
		get() = field
		set(value)
		{
			field = value

			if (hp < value) hp = value
		}

	var deathEffect: ParticleEffect? = null

	var blocking = false
	var invulnerable = false
	var godMode = false

	var tookDamage = false
	var blockedDamage = false
	var blockBroken = false

	lateinit var attackDefinition: AttackDefinition

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		maxHP += xml.getInt("HP")

		val deathEl = xml.getChildByName("Death")
		if (deathEl != null) deathEffect = AssetManager.loadParticleEffect(deathEl)

		val attackEl = xml.getChildByName("Attack")!!
		attackDefinition = AttackDefinition()
		attackDefinition.parse(attackEl)
	}

	fun dealDamage(amount: Float, blockable: Boolean)
	{
		val baseDam = amount

		unblockableDam = !blockable

		hp -= baseDam

		unblockableDam = !blockable
	}

	operator fun get(key: String, fallback: Float? = null): Float
	{
		return when (key.toLowerCase())
		{
			"hp" -> hp
			"maxhp" -> maxHP
			else -> fallback ?: throw Exception("Unknown statistic '$key'!")
		}
	}

	fun write(variableMap: ObjectFloatMap<String>, prefixName: String? = null): ObjectFloatMap<String>
	{
		val prefix = if (prefixName != null) "$prefixName." else ""

		variableMap.put(prefix + "hp", hp)
		variableMap.put(prefix + "maxhp", maxHP)

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
	lateinit var damage: Range
	var range: Int = 1
	var hitEffect: ParticleEffect? = null
	var flightEffect: ParticleEffect? = null

	fun parse(xml: XmlData)
	{
		damage = Range.parse(xml.get("Damage"))
		range = xml.getInt("Range", 1)

		val hitEffectEl = xml.getChildByName("HitEffect")
		if (hitEffectEl != null) hitEffect = AssetManager.loadParticleEffect(hitEffectEl)

		val flightEffectEl = xml.getChildByName("FlightEffect")
		if (flightEffectEl != null) flightEffect = AssetManager.loadParticleEffect(flightEffectEl)
	}
}