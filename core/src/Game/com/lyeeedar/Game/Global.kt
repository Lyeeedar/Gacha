package com.lyeeedar

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Components.free
import com.lyeeedar.Game.EntityData
import com.lyeeedar.Game.Faction
import com.lyeeedar.Game.GameData
import com.lyeeedar.Game.Level
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Screens.AbstractScreen
import com.lyeeedar.Systems.createEngine
import com.lyeeedar.Systems.level
import com.lyeeedar.UI.*
import com.lyeeedar.UI.Tooltip
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Settings
import ktx.collections.set

/**
 * Created by Philip on 04-Jul-16.
 */

class Global
{
	companion object
	{
		var resolveInstant = false

		lateinit var engine: Engine

		var data = GameData()

		fun setup()
		{
			Statics.setup()
			engine = createEngine()

			Colors.put("IMPORTANT", Color(0.6f, 1f, 0.9f, 1f))

			var faction = Faction.load("Adventurer/Adventurer")
			for (hero in faction.heroes)
			{
				if ((hero.rarity == Rarity.COMMON || hero.rarity == Rarity.UNCOMMON) && data.heroPool.size < 5)
				{
					data.heroPool.add(EntityData(hero, Ascension.MUNDANE, 1))
				}
			}
			data.unlockedFactions.add(faction)

			faction = Faction.load("Greenskin/GreenskinAlliance")
			data.unlockedFactions.add(faction)

			faction = Faction.load("Druid/Druid")
			data.unlockedFactions.add(faction)
		}

		fun newGame()
		{
			Statics.settings = Settings()

			val generator = MapGenerator.load("BanditHideout")
			generator.execute(29867)
		}

		fun changeLevel(level: Level)
		{
			loadLevel(level)
		}

		private fun loadLevel(level: Level)
		{
			engine.level?.destroyingLevel = true

			for (entity in engine.entities)
			{
				entity.free()
			}

			engine.removeAllEntities()
			EntityPool.flushFreedEntities()

			level.updateMetaRegions()

			engine.level = level

			val added = ObjectSet<Entity>()
			for (tile in level.grid)
			{
				for (slot in SpaceSlot.Values)
				{
					val entity = tile.contents[slot] ?: continue
					if (!added.contains(entity))
					{
						added.add(entity)

						engine.addEntity(entity)
					}
				}
			}
		}

		fun getExperienceForLevel(level: Int): Int
		{
			val expRequired = (100 * Math.pow(1.1, level.toDouble())).toInt()
			return expRequired
		}
	}
}