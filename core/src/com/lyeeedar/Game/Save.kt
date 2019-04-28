package com.lyeeedar.Game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Ascension
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Save
{
	companion object
	{
		val kryo: Kryo by lazy { initKryo() }
		fun initKryo(): Kryo
		{
			val kryo = Kryo()
			kryo.isRegistrationRequired = false

			kryo.registerGdxSerialisers()
			kryo.registerLyeeedarSerialisers()

			return kryo
		}

		fun save()
		{
			if (doingLoad) return

			val outputFile = Gdx.files.local("save.dat")

			val output: Output
			try
			{
				output = Output(GZIPOutputStream(outputFile.write(false)))
			}
			catch (e: Exception)
			{
				e.printStackTrace()
				return
			}

			val data = XmlData()
			data.name = "Save"
			data.set("Version", 1)

			val gameData = data.addChild("GameData")
			gameData.set("Zone", Global.data.currentZone)
			gameData.set("Progression", Global.data.currentZoneProgression)
			gameData.set("Gold", Global.data.gold)
			gameData.set("Experience", Global.data.experience)

			val equipment = data.addChild("Equipment")
			for (equip in Global.data.equipment)
			{
				val equipEl = equipment.addChild("Equip")
				equip.save(equipEl)
			}

			val factions = data.addChild("Factions")
			for (faction in Global.data.unlockedFactions)
			{
				factions.set(faction.name, faction.path)
			}

			val heroes = data.addChild("Heroes")
			for (hero in Global.data.heroPool)
			{
				val heroEl = heroes.addChild("Hero")
				heroEl.set("Faction", hero.factionEntity.faction.path)
				heroEl.set("Path", hero.factionEntity.entityPath)
				heroEl.set("Level", hero.level)
				heroEl.set("Ascension", hero.ascension.toString())
				heroEl.set("Shards", hero.ascensionShards)

				val equipmentEl = heroEl.addChild("Equipment")
				for (slot in EquipmentSlot.Values)
				{
					val equip = hero.equipment[slot] ?: continue

					val equipEl = equipmentEl.addChild(slot.toString())
					equip.save(equipEl)
				}
			}

			val lastSelectedHeroes = data.addChild("LastSelectedHeroes")
			for (i in 0 until 5)
			{
				lastSelectedHeroes.set("$i", Global.data.lastSelectedHeroes[i] ?: "---")
			}

			data.save(output)
			output.close()
		}

		var doingLoad = false
		fun load()
		{
			doingLoad = true
			var input: Input? = null

			try
			{
				val saveFileHandle = Gdx.files.local("save.dat")
				if (!saveFileHandle.exists())
				{
					doingLoad = false
					return
				}

				input = Input(GZIPInputStream(saveFileHandle.read()))

				val data = XmlData()
				data.load(input)

				val gameData = data.getChildByName("GameData")!!
				val zone = gameData.getInt("Zone")
				val progression = gameData.getInt("Progression")
				val gold = gameData.getInt("Gold")
				val experience = gameData.getInt("Experience")

				val equipmentEl = data.getChildByName("Equipment")!!
				val equipment = Array<Equipment>()
				for (equipEl in equipmentEl.children)
				{
					val equip = Equipment.load(equipEl)
					equipment.add(equip)
				}

				val factionsEl = data.getChildByName("Factions")!!
				val factions = Array<Faction>()
				for (factionEl in factionsEl.children)
				{
					val path = factionEl.value as String
					val faction = Faction.load(path)
					factions.add(faction)
				}

				val heroesEl = data.getChildByName("Heroes")!!
				val heroes = Array<EntityData>()
				for (heroEl in heroesEl.children)
				{
					val factionPath = heroEl.get("Faction")
					val heroPath = heroEl.get("Path")
					val level = heroEl.getInt("Level")
					val ascension = Ascension.valueOf(heroEl.get("Ascension"))
					val shards = heroEl.getInt("Shards")

					val entityData = EntityData()
					val faction = factions.firstOrNull { it.path == factionPath } ?: continue
					entityData.factionEntity = faction.heroes.firstOrNull { it.entityPath == heroPath } ?: continue
					entityData.level = level
					entityData.ascension = ascension
					entityData.ascensionShards = shards

					val equipmentEl = heroEl.getChildByName("Equipment")!!
					for (slot in EquipmentSlot.Values)
					{
						val equipEl = equipmentEl.getChildByName(slot.toString()) ?: continue
						val equip = Equipment.load(equipEl)
						entityData.equipment[slot] = equip
					}

					heroes.add(entityData)
				}

				val lastSelectedEl = data.getChildByName("LastSelectedHeroes")!!
				val lastSelected = kotlin.Array<String?>(5) { null }
				var i = 0
				for (el in lastSelectedEl.children)
				{
					if (el.value != "---")
					{
						lastSelected[i] = el.value as String
					}

					i++
				}

				// load successful, now write to game

				Global.data.currentZone = zone
				Global.data.currentZoneProgression = progression
				Global.data.gold = gold
				Global.data.experience = experience

				Global.data.equipment.clear()
				Global.data.equipment.addAll(equipment)

				Global.data.unlockedFactions.clear()
				Global.data.unlockedFactions.addAll(factions)

				Global.data.heroPool.clear()
				Global.data.heroPool.addAll(heroes)

				for (i in 0 until 5)
				{
					Global.data.lastSelectedHeroes[i] = lastSelected[i]
				}

				GameDataBar.complete()
			}
			catch (ex: Exception)
			{
				if (!Global.release)
				{
					throw ex
				}

				doingLoad = false
				return
			}
			finally
			{
				input?.close()
			}

			doingLoad = false
		}
	}
}