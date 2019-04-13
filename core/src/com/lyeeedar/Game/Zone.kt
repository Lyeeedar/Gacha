package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.pos
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Rarity
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class Zone(val seed: Long)
{
	val numEncounters = 40
	val width = 10
	val height = 15

	val ran = Random.obtainTS(seed)

	val factions = Array<Faction>()
	lateinit var grid: Array2D<ZoneTile>
	lateinit var levelRange: Range

	lateinit var floor1: SpriteWrapper
	lateinit var floor2: SpriteWrapper
	lateinit var path: SpriteWrapper

	lateinit var theme: Theme
	val possibleLevels = Array<XmlData>()

	lateinit var currentEncounter: ZoneTile

	fun createGrid()
	{
		grid = Array2D(width, height) { x, y -> ZoneTile(x, y) }
		for (tile in grid)
		{
			tile.sprite = if (ran.nextBoolean()) floor1.copy() else floor2.copy()
			tile.sprite.chooseSprites()
		}
	}

	fun createPath()
	{
		// place numEncounters random points, one bottom, one top
		val validPoints = Array<ZoneTile>()
		for (x in 1 until grid.width-1)
		{
			for (y in 2 until grid.height-1 step 2)
			{
				validPoints.add(grid[x, y])
			}
		}

		if (validPoints.size < numEncounters-2)
		{
			throw Exception("Grid too small!")
		}

		val points = Array<ZoneTile>()
		val w3 = width / 3
		points.add(grid[w3+ran.nextInt(grid.width-w3*2), 0])
		points.add(grid[w3+ran.nextInt(grid.width-w3*2), grid.height-1])

		while (points.size < numEncounters)
		{
			points.add(validPoints.removeRandom(ran))
		}

		// create array of points at each y
		val pointsPerY = kotlin.Array<Array<ZoneTile>?>(grid.height) { null }
		for (y in 0 until grid.height)
		{
			pointsPerY[y] = points.filter { it.y == y }.toGdxArray()
			if (pointsPerY[y]!!.size == 0)
			{
				pointsPerY[y] = null
			}
		}

		// starting at the bottom center walk through points and link together
		var y = 1
		var current = pointsPerY[0]!![0]
		var left = current.x < width / 2

		while (true)
		{
			// find the next row with points
			while (true)
			{
				if (pointsPerY[y] == null)
				{
					y++
				}
				else
				{
					break
				}
			}

			// sort based on going left or right
			val currentPoints = pointsPerY[y]!!
			val sorted = if (left) currentPoints.sortedBy { it.x }.toGdxArray() else currentPoints.sortedByDescending { it.x }.toGdxArray()

			// link up the points in order
			for (point in sorted)
			{
				current.nextTile = point

				// draw path between points
				val path = BresenhamLine.lineNoDiag(current.x, current.y, point.x, point.y, grid)
				for (p in path)
				{
					val t = grid[p]
					t.sprite = this.path.copy()
					t.sprite.chooseSprites()
				}

				current = point
			}

			y++
			left = !left

			// if weve reached the end, break
			if (y == grid.height)
			{
				break
			}
		}

		current = pointsPerY[0]!![0]
		var progression = 0
		while (true)
		{
			val alpha = progression.toFloat() / numEncounters.toFloat()
			val level = levelRange.min.lerp(levelRange.max, alpha).toInt()

			current.encounter = createEncounter(level, progression)

			if (current.nextTile == null)
			{
				break
			}

			progression++
			current = current.nextTile
		}

		pointsPerY[0]!![0].isCurrent = true
		currentEncounter = pointsPerY[0]!![0]

		for (point in points)
		{
			point.isEncounter = true
			point.updateFlag()
		}
	}

	fun createEncounter(level: Int, progression: Int): Encounter
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

		val encounter = Encounter()
		encounter.gridEl = possibleLevels.random(ran)

		for (i in 0 until 5)
		{
			val level = max(1, level + ran.nextInt(-3, 3))
			val heroData = entities.removeRandom(ran)
			val entityData = EntityData(heroData, Ascension.MUNDANE, level)

			encounter.enemies.add(entityData)
		}

		return encounter
	}

	companion object
	{
		fun load(path: String): Zone
		{
			val xml = getXml(path)

			val zone = Zone(Random.random.nextLong())

			val factionsEl = xml.getChildByName("Factions")!!
			for (el in factionsEl.children)
			{
				zone.factions.add(Faction.load(el.text))
			}

			zone.floor1 = SpriteWrapper.load(xml.getChildByName("Floor1")!!)
			zone.floor2 = SpriteWrapper.load(xml.getChildByName("Floor2")!!)
			zone.path = SpriteWrapper.load(xml.getChildByName("Path")!!)

			zone.levelRange = Range.parse(xml.get("LevelRange"))

			zone.theme = Theme.load(xml.get("Theme"))

			val levelsEl = xml.getChildByName("Levels")!!
			for (el in levelsEl.children)
			{
				zone.possibleLevels.add(el)
			}

			zone.createGrid()
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
	var isComplete = false
	var isCurrent = false

	var nextTile: ZoneTile? = null
	var encounter: Encounter? = null

	var flagSprite: Sprite? = null

	lateinit var sprite: SpriteWrapper

	fun updateFlag()
	{
		if (isEncounter)
		{
			if (isCurrent)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_combat", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else if (isComplete)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_complete", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_enemy", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
		}
	}
}

class Encounter
{
	val enemies = Array<EntityData>(5)
	lateinit var gridEl: XmlData

	fun createLevel(theme: Theme): Level
	{
		val level = Level.load(gridEl, theme)

		var i = 0
		for (enemy in enemies)
		{
			val entity = enemy.getEntity("2")
			level.enemyTiles[i].entity = entity
			entity.pos().tile = level.enemyTiles[i].tile
			entity.pos().addToTile(entity)

			i++
		}

		return level
	}
}