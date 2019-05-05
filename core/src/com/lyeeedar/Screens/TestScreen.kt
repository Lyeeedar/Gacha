package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Faction
import com.lyeeedar.Game.FactionEntity
import com.lyeeedar.Global
import com.lyeeedar.Tests.DPSTest
import com.lyeeedar.Tests.HeroEntry
import com.lyeeedar.Tests.RatingTest
import com.lyeeedar.UI.Seperator
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.filename
import com.lyeeedar.Util.toString
import ktx.collections.set

class TestScreen : AbstractScreen()
{
	val ratingTest = RatingTest()
	val heroes = ObjectMap<String, HeroEntry>()

	override fun create()
	{
		val pathToFactionEntityMap = ObjectMap<String, FactionEntity>()
		for (factionPath in XmlData.enumeratePaths("Factions", "Faction"))
		{
			val faction = Faction.load(factionPath.replace("Factions/", ""))
			for (entity in faction.heroes)
			{
				pathToFactionEntityMap[entity.entityPath + ".xml"] = entity
			}
		}

		for (entity in XmlData.enumeratePaths("Factions", "Entity"))
		{
			if (!entity.toLowerCase().contains("test"))
			{
				var rarity = "---"
				if (pathToFactionEntityMap.containsKey(entity))
				{
					val fe = pathToFactionEntityMap[entity]
					rarity = fe.rarity.shortName
				}
				else
				{
					continue
				}

				val entry = HeroEntry(entity, rarity)
				heroes[entity] = entry

				entry.damage = DPSTest().testEntity(entity)
			}
		}

		updateTables()
	}

	fun updateTables()
	{
		mainTable.clear()
		mainTable.add(Label("Name", Global.skin, "title"))
		mainTable.add(Label("W/L", Global.skin, "title"))
		mainTable.add(Label("Dam/DPS", Global.skin, "title"))
		mainTable.add(Label("Rarity", Global.skin, "title"))
		mainTable.row()
		mainTable.add(Seperator(Global.skin)).colspan(4).growX()
		mainTable.row()

		for (hero in heroes.values().sortedByDescending { it.wins.toFloat() / it.losses.toFloat() })
		{
			mainTable.add(Label(hero.path.filename(false), Global.skin, "small"))
			mainTable.add(Label((hero.wins.toFloat() / hero.losses.toFloat()).toString(2), Global.skin, "small"))
			mainTable.add(Label(hero.damage.toString(2) + " / " + (hero.totalDps / hero.numSamples).toString(2), Global.skin, "small"))
			mainTable.add(Label(hero.rarity, Global.skin, "small"))
			mainTable.row()
		}
	}

	var count = 0
	var offAccumulator = 0f

	var accumulator = 0f
	override fun doRender(delta: Float)
	{
		accumulator += delta
		if (accumulator > 1f)
		{
			updateTables()
			accumulator = 0f
		}

		if (offAccumulator > 0f)
		{
			offAccumulator += delta
			if (offAccumulator > 1f)
			{
				offAccumulator = -1f
			}
		}
		else
		{
			for (i in 0 until 50)
			{
				ratingTest.doRatingTest(heroes)
			}

			count++
			if (count == 4)
			{
				count = 0
				offAccumulator = delta
			}
		}

		System.gc()
	}
}