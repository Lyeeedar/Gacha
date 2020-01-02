package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Faction
import com.lyeeedar.Game.FactionEntity
import com.lyeeedar.Tests.DPSTest
import com.lyeeedar.Tests.HeroEntry
import com.lyeeedar.Tests.RatingTest
import com.lyeeedar.UI.Seperator
import com.lyeeedar.Util.*
import ktx.collections.set

class TestScreen : AbstractScreen()
{
	val ratingTest = RatingTest()
	val heroes = ObjectMap<String, HeroEntry>()

	override fun create()
	{
		drawFPS = false

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

		val titleTable = Table()

		val nameLabel = Label("Name", Statics.skin)
		titleTable.add(nameLabel).growX()
		val wlLabel = Label("W/L", Statics.skin)
		titleTable.add(wlLabel).width(nameLabel.prefWidth)
		val damdpsLabel = Label("Dam/DPS", Statics.skin)
		titleTable.add(damdpsLabel).width(damdpsLabel.prefWidth)
		val rarityLabel = Label("Rarity", Statics.skin)
		titleTable.add(rarityLabel).width(rarityLabel.prefWidth)

		mainTable.add(titleTable).growX()
		mainTable.row()
		mainTable.add(Seperator(Statics.skin)).colspan(4).growX()
		mainTable.row()

		var bright = true
		for (hero in heroes.values().sortedByDescending { it.wins.toFloat() / it.losses.toFloat() })
		{
			val rowTable = Table()
			if (bright)
			{
				rowTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
			}
			bright = !bright

			rowTable.add(Label(hero.path.filename(false), Statics.skin, "small")).growX()
			rowTable.add(Label((hero.wins.toFloat() / hero.losses.toFloat()).toString(2), Statics.skin, "small")).width(nameLabel.prefWidth)
			rowTable.add(Label(hero.damage.toString(2) + " / " + (hero.totalDps / hero.numSamples).toString(2), Statics.skin, "small")).width(damdpsLabel.prefWidth)
			rowTable.add(Label(hero.rarity, Statics.skin, "small")).width(rarityLabel.prefWidth)

			mainTable.add(rowTable).growX()
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