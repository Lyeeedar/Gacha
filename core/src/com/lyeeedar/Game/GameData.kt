package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.stats
import com.lyeeedar.Global
import com.lyeeedar.Systems.directionSprite

class GameData
{
	val heroPool = Array<EntityData>()
}

class EntityData()
{
	lateinit var factionEntity: FactionEntity
	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 0

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
		stats.resetHP()

		Global.engine.directionSprite().processEntity(entity, 0f)

		return entity
	}
}