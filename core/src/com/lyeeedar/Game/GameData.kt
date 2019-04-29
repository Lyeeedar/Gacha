package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.stats
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.EquipmentWeight
import com.lyeeedar.Global
import com.lyeeedar.Systems.directionSprite
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.random
import java.lang.System.currentTimeMillis

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

	var lastRefreshTime: Long = 0
	val currentShopEquipment = kotlin.Array<Equipment?>(4) { null }
	val currentShopHeroes = kotlin.Array<String?>(4) { null }
	lateinit var currentShopFaction: Faction
	lateinit var currentShopWeight: EquipmentWeight

	val lastRefreshTimeMillis: Long
		get() = lastRefreshTime * refreshTimeMillis

	val refreshTimeMillis = 6L * 60L * 60L * 1000L

	fun refresh()
	{
		val currentTime = currentTimeMillis()
		val asStep = currentTime / refreshTimeMillis

		if (lastRefreshTime != asStep)
		{
			lastRefreshTime = asStep

			for (i in 0 until 4)
			{
				currentShopEquipment[i] = EquipmentCreator.createRandom(getCurrentLevel())
			}

			for (i in 0 until 4)
			{
				val possibleDrops = Array<FactionEntity>()
				for (faction in Global.data.unlockedFactions)
				{
					for (hero in faction.heroes)
					{
						if (Global.data.heroPool.any{ it.factionEntity == hero})
						{
							for (i in 0 until hero.rarity.dropRate)
							{
								possibleDrops.add(hero)
							}
						}
					}
				}

				val hero = possibleDrops.random()
				val heroData = Global.data.heroPool.first { it.factionEntity == hero }

				currentShopHeroes[i] = heroData.factionEntity.entityPath
			}

			currentShopFaction = unlockedFactions.random()
			currentShopWeight = EquipmentWeight.Values.random()
		}
	}

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