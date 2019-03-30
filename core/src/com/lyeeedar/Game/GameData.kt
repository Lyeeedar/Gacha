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
	var level: Int = 1
}

class EntityData()
{
	lateinit var factionEntity: FactionEntity
	var ascension: Ascension = Ascension.MUNDANE

	constructor(entity: FactionEntity, ascension: Ascension) : this()
	{
		this.factionEntity = entity
		this.ascension = ascension
	}

	fun getEntity(gameData: GameData): Entity
	{
		val entity = EntityLoader.load(factionEntity.entityPath)
		val stats = entity.stats()!!
		stats.ascension = ascension
		stats.level = gameData.level
		stats.faction = "1"
		stats.factionData = factionEntity.faction
		stats.resetHP()

		Global.engine.directionSprite().processEntity(entity, 0f)

		return entity
	}
}