package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

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

			field = v
			if (godMode && field < 1) field = 1f
		}

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

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		maxHP += xml.getInt("HP")

		val deathEl = xml.getChildByName("Death")
		if (deathEl != null) deathEffect = AssetManager.loadParticleEffect(deathEl)
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
