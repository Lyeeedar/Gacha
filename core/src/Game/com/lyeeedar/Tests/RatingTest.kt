package com.lyeeedar.Tests

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class HeroEntry(val path: String, val rarity: String, var wins: Int = 0, var losses: Int = 0, var damage: Float = 0f)
{
	var totalDps: Float = 0f
	var numSamples: Int = 0
}

class RatingTest
{
	fun doRatingTest(heroes: ObjectMap<String, HeroEntry>)
	{
		Global.resolveInstant = true

		val level = Level.Companion.load("Levels/Test")

		val playerHeroes = Array<HeroEntry>(5)
		val enemyHeroes = Array<HeroEntry>(5)

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

		var iteration = 0
		var winningFaction: String? = null
		while (iteration < 1000 && winningFaction == null)
		{
			Global.engine.update(1f)
			Future.update(1f)

			winningFaction = level.isComplete()
			iteration++
		}

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

			data.totalDps += entity.stats().damageDealt / iteration.toFloat()
			data.numSamples++
		}

		for (i in 0 until enemyHeroes.size)
		{
			val data = enemyHeroes[i]
			val entity = level.enemyTiles[i].entity!!

			data.totalDps += entity.stats().damageDealt / iteration.toFloat()
			data.numSamples++
		}

		Global.resolveInstant = false
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
				val entry = HeroEntry(entity, "--")
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
			println(entry.path + ": 			Wins: " + entry.wins + "  Losses: " + entry.losses + "   DPS: " + (entry.totalDps / entry.numSamples))
		}

		Global.engine.addSystem(renderSystem)
	}
}