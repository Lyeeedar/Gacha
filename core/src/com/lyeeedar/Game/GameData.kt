package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.stats
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.Systems.directionSprite
import com.lyeeedar.Util.FastEnumMap

class GameData
{
	val heroPool = Array<EntityData>()
	val unlockedFactions = Array<Faction>()

	val equipment = Array<Equipment>()

	val lastSelectedHeroes = kotlin.Array<String?>(5) { null }

	var experience = 0
	var gold = 0

	var currentZone: Int = 1
	var currentZoneProgression: Int = 0

	fun getCurrentLevel(): Int
	{
		val zoneStartLevel = (currentZone-1) * Zone.zoneLevelRange + 1

		val alpha = currentZoneProgression.toFloat() / Zone.numEncounters.toFloat()

		return zoneStartLevel + (Zone.zoneLevelRange.toFloat() * alpha).toInt()
	}
}

class EntityData()
{
	lateinit var factionEntity: FactionEntity
	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 0

	val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	var ascensionShards = 0

	constructor(entity: FactionEntity, ascension: Ascension, level: Int) : this()
	{
		this.factionEntity = entity
		this.ascension = ascension
		this.level = level
	}

	fun getEntity(faction: String): Entity
	{
		val entity = EntityLoader.load(factionEntity.entityPath)
		val stats = entity.stats()!!
		stats.ascension = ascension
		stats.level = level
		stats.faction = faction
		stats.factionData = factionEntity.faction

		for (slot in EquipmentSlot.Values)
		{
			stats.equipment[slot] = equipment[slot]
		}

		stats.resetHP()

		Global.engine.directionSprite().processEntity(entity, 0f)

		return entity
	}
}