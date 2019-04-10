package com.lyeeedar.Tests

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.stats
import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class HeroEntry(val path: String, var wins: Int = 0, var losses: Int = 0, var damage: Float = 0f, var averageDps: Float = 1f)

class RatingTest
{
	fun doRatingTest(heroes: ObjectMap<String, HeroEntry>)
	{
		val level = Level.Companion.load("Levels/Test")

		val playerHeroes = Array<HeroEntry>()
		val enemyHeroes = Array<HeroEntry>()

		for (pos in level.playerTiles)
		{
			val hero = heroes.values().toArray().random()
			playerHeroes.add(hero)

			val entity = EntityLoader.load(hero.path)
			pos.entity = entity

			entity.pos().tile = pos.tile
			entity.pos().addToTile(entity)

			entity.stats().faction = "1"
		}

		for (pos in level.enemyTiles)
		{
			val hero = heroes.values().toArray().random()
			enemyHeroes.add(hero)

			val entity = EntityLoader.load(hero.path)
			pos.entity = entity

			entity.pos().tile = pos.tile
			entity.pos().addToTile(entity)

			entity.stats().faction = "2"
		}

		Global.changeLevel(level)
		level.begin()

		Global.resolveInstant = true
		var iteration = 0
		var winningFaction: String? = null
		while (iteration < 1000 && winningFaction == null)
		{
			Global.engine.update(1f)
			Future.update(1f)

			winningFaction = level.isComplete()
			iteration++
		}
		Global.resolveInstant = false

		if (winningFaction != null)
		{
			if (winningFaction == "1")
			{
				for (entry in playerHeroes)
				{
					entry.wins++
				}

				for (entry in enemyHeroes)
				{
					entry.losses++
				}
			}
			else
			{
				for (entry in playerHeroes)
				{
					entry.losses++
				}

				for (entry in enemyHeroes)
				{
					entry.wins++
				}
			}
		}

		for (i in 0 until playerHeroes.size)
		{
			val data = playerHeroes[i]
			val entity = level.playerTiles[i].entity!!

			data.averageDps = (data.averageDps + (entity.stats().damageDealt / iteration.toFloat())) / 2f
		}

		for (i in 0 until enemyHeroes.size)
		{
			val data = enemyHeroes[i]
			val entity = level.enemyTiles[i].entity!!

			data.averageDps = (data.averageDps + (entity.stats().damageDealt / iteration.toFloat())) / 2f
		}
	}

	fun runRatingTest()
	{
		val renderSystem = Global.engine.render()
		Global.engine.removeSystem(renderSystem)

		val heroes = ObjectMap<String, HeroEntry>()
		for (entity in XmlData.enumeratePaths("Factions", "Entity"))
		{
			if (!entity.toLowerCase().contains("test"))
			{
				val entry = HeroEntry(entity)
				heroes[entity] = entry
			}
		}

		for (i in 0 until 1000)
		{
			doRatingTest(heroes)
		}

		val sorted = heroes.values().sortedByDescending { it.wins.toFloat() / it.losses.toFloat() }
		for (entry in sorted)
		{
			println(entry.path + ": 			Wins: " + entry.wins + "  Losses: " + entry.losses + "   DPS: " + entry.averageDps)
		}

		Global.engine.addSystem(renderSystem)
	}
}