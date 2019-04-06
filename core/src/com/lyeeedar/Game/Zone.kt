package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class Zone(val seed: Long)
{
	val numEncounters = 40
	val width = 25
	val height = 30

	val ran = Random.obtainTS(seed)

	val factions = Array<Faction>()
	lateinit var grid: Array2D<ZoneTile>

	lateinit var floor1: SpriteWrapper
	lateinit var floor2: SpriteWrapper
	lateinit var path: SpriteWrapper

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
			for (y in 3 until grid.height-1 step 3)
			{
				validPoints.add(grid[x, y])
			}
		}

		if (validPoints.size < numEncounters-2)
		{
			throw Exception("Grid too small!")
		}

		val points = Array<ZoneTile>()
		points.add(grid[10+ran.nextInt(grid.width-20), 0])
		points.add(grid[10+ran.nextInt(grid.width-20), grid.height-1])

		while (points.size < numEncounters)
		{
			points.add(validPoints.removeRandom(ran))
		}

		for (point in points)
		{
			point.isEncounter = true
			point.flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag", drawActualSize = true)
			point.flagSprite!!.randomiseAnimation()
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
	var nextTile: ZoneTile? = null

	var flagSprite: Sprite? = null

	lateinit var sprite: SpriteWrapper
}