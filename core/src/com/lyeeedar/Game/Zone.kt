package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.lyeeedar.*
import com.lyeeedar.Components.directionalSprite
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.stats
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTable
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class Zone(val zoneIndex: Int, val handicap: Float)
{
	val ran = Random.obtainTS(20181201L + zoneIndex)

	val factions = Array<Faction>()
	lateinit var grid: Array2D<ZoneTile>
	val levelRange: Range = Range(Point((zoneIndex - 1) * zoneLevelRange + 1, zoneIndex * zoneLevelRange))

	lateinit var floor1: SpriteWrapper
	lateinit var floor2: SpriteWrapper
	lateinit var path: SpriteWrapper

	lateinit var theme: Theme
	val possibleLevels = Array<XmlData>()

	lateinit var encounters: kotlin.Array<Encounter>

	val width: Int
		get() = grid.width

	val height: Int
		get() = grid.height

	fun createGrid(xmlData: XmlData)
	{
		val charGrid = xmlData.toCharGrid()

		grid = Array2D(charGrid.width, charGrid.height) { x, y -> ZoneTile(x, y) }

		var foundEncounters = 0
		for (tile in grid)
		{
			val char = charGrid[tile.x, grid.height-tile.y-1]

			tile.sprite = if (char == '#') floor2.copy() else floor1.copy()
			tile.sprite.chooseSprites()

			if (char == '!')
			{
				tile.isEncounter = true
				foundEncounters++
			}
			else if (char == 'S')
			{
				tile.isEncounter = true
				tile.isStart = true
				foundEncounters++
			}
			else if (char == 'B')
			{
				tile.isEncounter = true
				tile.isBoss = true
				foundEncounters++
			}
			else if (char == ',')
			{
				tile.sprite = path.copy()
				tile.sprite.chooseSprites()
			}
		}

		if (foundEncounters != numEncounters)
		{
			throw Exception("Invalid number of encounters defined in zone! Found $foundEncounters, should have found $numEncounters")
		}
	}

	fun createPath()
	{
		// starting at the start tile, walk to neighbouring tile, building the path
		val explored = IntSet()

		var current = grid.first{ it.isStart }
		var progression = 0
		val encountersArray = Array<Encounter>()
		while (true)
		{
			explored.add(current.hashCode())

			val alpha = progression.toFloat() / numEncounters.toFloat()
			val level = levelRange.min.lerp(levelRange.max, alpha)

			val encounter = createEncounter(level, progression, current.isBoss)
			encountersArray.add(encounter)
			current.encounter = encounter

			current.sprite = path.copy()
			current.sprite.chooseSprites()

			if (progression == numEncounters-1)
			{
				break
			}

			var found = false
			for (dir in Direction.ValuesWithoutCenter)
			{
				val nTile = grid.tryGet(current.x + dir.x, current.y + dir.y, null) ?: continue
				if (!explored.contains(nTile.hashCode()) && nTile.isEncounter)
				{
					current = nTile
					found = true
					break
				}
			}

			if (!found)
			{
				throw Exception("Unable to find next encounter! Progression: $progression, Position: $current")
			}

			progression++
		}

		encounters = kotlin.Array(encountersArray.size) { i -> encountersArray[i] }

		updateFlags()
	}

	fun createEncounter(level: Float, progression: Int, isBoss: Boolean): Encounter
	{
		val entities = Array<FactionEntity>()
		for (faction in factions)
		{
			for (entity in faction.heroes)
			{
				if (entity.rarity.ordinal >= Rarity.RARE.ordinal && progression < numEncounters*0.4) continue
				if (entity.rarity.ordinal >= Rarity.SUPERRARE.ordinal && progression < numEncounters*0.6) continue
				if (entity.rarity.ordinal >= Rarity.ULTRARARE.ordinal && progression < numEncounters*0.8) continue

				for (i in 0 until entity.rarity.spawnWeight)
				{
					entities.add(entity)
				}
			}
		}

		val encounter = Encounter(this, level.toInt(), progression, isBoss, ran.nextLong())
		encounter.gridEl = possibleLevels.random(ran)

		val levels = kotlin.Array<Int>(5) { level.toInt() }
		val levelRaisedPool = (0..4).toGdxArray()
		var remainder = level - levels[0]
		while (remainder >= 0.2f)
		{
			remainder -= 0.2f
			levels[levelRaisedPool.removeRandom(ran)]++
		}

		for (i in 0 until 5)
		{
			val level = levels[i]
			val heroData = entities.removeRandom(ran)
			val entityData = EntityData(heroData, Ascension.MUNDANE, level)

			encounter.enemies.add(entityData)
		}

		return encounter
	}

	fun updateFlags()
	{
		for (tile in grid)
		{
			tile.updateFlag()
		}
	}

	companion object
	{
		val numEncounters = 40
		val zoneLevelRange = 20

		fun load(index: Int): Zone
		{
			val path = "Zones/Zone$index"
			val xml = getXml(path)

			val zone = Zone(index, -0.3f)

			val factionsEl = xml.getChildByName("Factions")!!
			for (el in factionsEl.children)
			{
				zone.factions.add(Faction.load(el.text))
			}

			val gridEl = xml.getChildByName("Grid")!!

			zone.floor1 = SpriteWrapper.load(xml.getChildByName("Floor1")!!)
			zone.floor2 = SpriteWrapper.load(xml.getChildByName("Floor2")!!)
			zone.path = SpriteWrapper.load(xml.getChildByName("Path")!!)

			zone.theme = Theme.load(xml.get("Theme"))

			val levelsEl = xml.getChildByName("Levels")!!
			for (el in levelsEl.children)
			{
				zone.possibleLevels.add(el)
			}

			zone.createGrid(gridEl)
			zone.createPath()

			return zone
		}
	}
}

class ZoneTile(x: Int, y: Int) : Point(x, y), IPathfindingTile
{
	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		return true
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?): Int
	{
		return 0
	}

	var isEncounter = false
	var isBoss = false
	var isStart = false

	var encounter: Encounter? = null

	var flagSprite: Sprite? = null

	lateinit var sprite: SpriteWrapper

	fun updateFlag()
	{
		if (isEncounter)
		{
			if (encounter!!.progression == Global.data.currentZoneProgression)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_combat", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else if (encounter!!.progression < Global.data.currentZoneProgression)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_complete", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_enemy", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}

			if (isBoss)
			{
				flagSprite!!.baseScale[0] = 1.5f
				flagSprite!!.baseScale[1] = 1.5f
				flagSprite!!.colour = Colour(1f, 0.9f, 0.9f, 1f)
			}
			else
			{
				//flagSprite!!.baseScale[0] = 0.8f
				//flagSprite!!.baseScale[1] = 0.8f
			}
		}
	}
}

class Encounter(val zone: Zone, val level: Int, val progression: Int, val isBoss: Boolean, val seed: Long)
{
	val enemies = Array<EntityData>(5)
	lateinit var gridEl: XmlData

	fun createLevel(theme: Theme): Level
	{
		val level = Level.load(gridEl, theme)

		val ran = Random.obtainTS(seed)
		val bossIndex = if (isBoss) ran.nextInt(5) else -1

		var i = 0
		for (enemy in enemies)
		{
			val entity = enemy.getEntity("2")
			level.enemyTiles[i].entity = entity
			entity.pos().tile = level.enemyTiles[i].tile
			entity.pos().addToTile(entity)
			entity.stats().statModifier = zone.handicap

			if (i == bossIndex)
			{
				entity.stats().statModifier += 0.2f
				entity.directionalSprite().directionalSprite.scale = 1.3f
			}

			i++
		}

		ran.freeTS()

		return level
	}

	class RewardDesc(val tile: Table, val func: () -> Unit)
	fun generateRewards(victory: Boolean): Array<RewardDesc>
	{
		val ran = Random.obtainTS(seed+100)

		val output = Array<RewardDesc>()

		// experience
		val expRequired = (100 * Math.pow(1.2, level.toDouble())).toInt()
		var expReward = (expRequired / 5f).toInt()
		expReward += (expReward * ((ran.nextFloat() * 0.4f) - 0.2f)).toInt() // +/-20%

		if (!victory)
		{
			expReward = (expReward * 0.4f).toInt()
		}

		val expTile = Stack()
		expTile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), 64f, 64f))
		expTile.add(SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_blue"), 64f, 64f))

		val expAmountTable = Table()
		expAmountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		expAmountTable.add(Label(expReward.prettyPrint(), Global.skin, "small")).padBottom(5f)

		expTile.addTable(expAmountTable).expandY().growX().bottom()
		expTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 64f, 64f))

		val expTable = Table()
		expTable.add(expTile).grow()
		expTable.addTapToolTip("Gain ${expReward.prettyPrint()} experience.")

		output.add(RewardDesc(expTable, { Global.data.experience += expReward }))

		// gold
		var goldReward = 500 + level * 2
		goldReward += (goldReward * ((ran.nextFloat() * 0.4f) - 0.2f)).toInt() // +/-20%

		if (!victory)
		{
			goldReward = (goldReward * 0.4f).toInt()
		}

		val goldTile = Stack()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), 64f, 64f))
		goldTile.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 64f, 64f))

		val goldAmountTable = Table()
		goldAmountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		goldAmountTable.add(Label(goldReward.prettyPrint(), Global.skin, "small")).padBottom(5f)

		goldTile.addTable(goldAmountTable).expandY().growX().bottom()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 64f, 64f))

		val goldTable = Table()
		goldTable.add(goldTile).grow()
		expTable.addTapToolTip("Gain ${goldReward.prettyPrint()} gold.")

		output.add(RewardDesc(goldTable, { Global.data.gold += goldReward }))

		// chance for equipment
		if (victory && ran.nextFloat() < 0.2f)
		{
			val equip = EquipmentCreator.createRandom(level)
			val tile = equip.createTile(64f)
			tile.addTapToolTip("Gain the equipment ${equip.fullName}.")

			output.add(RewardDesc(tile, { Global.data.equipment.add(equip) }))
		}

		ran.freeTS()

		return output
	}
}