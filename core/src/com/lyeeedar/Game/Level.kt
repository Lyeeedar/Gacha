package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.set

// ----------------------------------------------------------------------
class Level(grid: Array2D<Tile>)
{
	// ----------------------------------------------------------------------
	var grid: Array2D<Tile> = grid
		set(value)
		{
			field = value

			for (x in 0 until width)
			{
				for (y in 0 until height)
				{
					val tile = grid[x, y]

					tile.level = this
					tile.x = x
					tile.y = y

					for (dir in Direction.Values)
					{
						tile.neighbours.put( dir, getTile(x, y, dir) )
					}
				}
			}

			recalculateGrids()
		}

	// ----------------------------------------------------------------------
	var doubleGrid: Array<DoubleArray> = Array(0) { i -> DoubleArray(0) { i -> 0.0 } }
	var charGrid: Array<CharArray> = Array(0) { i -> CharArray(0) { i -> '.' } }

	// ----------------------------------------------------------------------
	fun recalculateGrids()
	{
		doubleGrid = Array(width) { i -> DoubleArray(height) { i -> 0.0 } }
		charGrid = Array(width) { i -> CharArray(height) { i -> '.' } }

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				if (grid[x, y].contents.containsKey(SpaceSlot.WALL))
				{
					doubleGrid[x][y] = 1.0
					charGrid[x][y] = '#'
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	val width: Int
		get() = grid.xSize

	// ----------------------------------------------------------------------
	val height: Int
		get() = grid.ySize

	// ----------------------------------------------------------------------
	val ambient: Colour = Colour(0.1f, 0.1f, 0.1f, 1f)

	// ----------------------------------------------------------------------
	var destroyingLevel = false

	// ----------------------------------------------------------------------
	val screenSpaceEffects = com.badlogic.gdx.utils.Array<ParticleEffect>()

	// ----------------------------------------------------------------------
	val metaregions = ObjectMap<String, com.badlogic.gdx.utils.Array<Tile>>()

	// ----------------------------------------------------------------------
	init
	{
		Global.engine.addEntityListener(Family.all(MetaRegionComponent::class.java).get(), object : EntityListener {
			override fun entityRemoved(entity: Entity?)
			{
				if (destroyingLevel) return

				Future.call( {updateMetaRegions()}, 0.5f, metaregions)
			}

			override fun entityAdded(entity: Entity?)
			{
				if (destroyingLevel) return

				Future.call( {updateMetaRegions()}, 0.5f, metaregions)
			}
		})
	}

	// ----------------------------------------------------------------------
	fun updateMetaRegions()
	{
		metaregions.clear()

		for (tile in grid)
		{
			for (entity in tile.contents)
			{
				if (entity.metaregion() != null)
				{
					for (key in entity.metaregion().keys)
					{
						if (!metaregions.containsKey(key))
						{
							metaregions[key] = com.badlogic.gdx.utils.Array()
						}

						if (!metaregions[key].contains(tile)) metaregions[key].add(tile)
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	fun getClosestMetaRegion(key: String, point: Point): Tile?
	{
		val lkey = key.toLowerCase()

		if (!metaregions.containsKey(lkey)) return null

		val points = metaregions[lkey]

		return points.sortedBy { point.taxiDist(it) }.first()
	}

	// ----------------------------------------------------------------------
	inline fun getTileClamped(point: Point) = getTile(MathUtils.clamp(point.x, 0, width-1), MathUtils.clamp(point.y, 0, height-1))!!

	// ----------------------------------------------------------------------
	inline fun getTile(point: Point) = getTile(point.x, point.y)

	// ----------------------------------------------------------------------
	inline fun getTile(point: Point, ox:Int, oy:Int) = getTile(point.x + ox, point.y + oy)

	// ----------------------------------------------------------------------
	inline fun getTile(point: Point, o: Point) = getTile(point.x + o.x, point.y + o.y)

	// ----------------------------------------------------------------------
	inline fun getTile(x: Int, y: Int, dir: Direction) = getTile(x + dir.x, y + dir.y)

	// ----------------------------------------------------------------------
	inline fun getTile(point: Point, dir: Direction) = getTile(point.x + dir.x, point.y + dir.y)

	// ----------------------------------------------------------------------
	fun getTile(x: Int, y: Int): Tile? = grid[x, y, null]

	// ----------------------------------------------------------------------
	companion object
	{
		fun load(path: String): Level
		{
			val xml = getXml(path)

			val charGrid: Array2D<Char>
			val gridEl = xml.getChildByName("Grid")!!
			val width = gridEl.getChild(0).text.length
			val height = gridEl.childCount
			charGrid = Array2D<Char>(width, height) { x, y -> gridEl.getChild(y).text[x] }

			val symbolsMap = IntMap<Symbol>()
			val symbolsEl = xml.getChildByName("Symbols")
			if (symbolsEl != null)
			{
				for (symbolEl in symbolsEl.children)
				{
					val symbol = Symbol.parse(symbolEl)
					symbolsMap[symbol.char.toInt()] = symbol
				}
			}

			val theme = Theme.load(xml.get("Theme"))
			for (symbol in theme.symbols)
			{
				if (!symbolsMap.containsKey(symbol.char.toInt())) // level overrides theme
				{
					symbolsMap[symbol.char.toInt()] = symbol
				}
			}

			val pathSymbol = symbolsMap['.'.toInt()]
			val groundSymbol = symbolsMap['#'.toInt()]

			var hasNemora = false
			var hasKhasos = false
			fun loadTile(tile: Tile, char: Char)
			{
				if (symbolsMap.containsKey(char.toInt()))
				{
					val symbol = symbolsMap[char.toInt()]
					if (symbol.extends != ' ')
					{
						loadTile(tile, symbol.extends)
					}
					else if (char != '.')
					{
						loadTile(tile, '.')
					}

					if (symbol.sprite != null)
					{
						tile.sprite = symbol.sprite.copy()
					}
				}
				else if (char.isDigit())
				{
					tile.sprite = groundSymbol.sprite!!.copy()

					val toSpawn: String
					if (char == '2')
					{
						if (Random.random(3) == 0)
						{
							toSpawn = "ShieldGoblin"
						}
						else
						{
							toSpawn = "Goblin"
						}
					}
					else
					{
						if (!hasNemora)
						{
							hasNemora = true
							toSpawn = "Nemora"
						}
						else if (!hasKhasos)
						{
							hasKhasos = true
							toSpawn = "Khasos"
						}
						else if (Random.random(4) == 0)
						{
							toSpawn = "Archer"
						}
						else
						{
							toSpawn = "Test1"
						}
					}

					val entity = EntityLoader.load(toSpawn)
					entity.stats()!!.faction = char.toString()

					tile.contents[entity.pos().slot] = entity
					entity.pos().tile = tile
				}
				else
				{
					tile.sprite = groundSymbol.sprite!!.copy()
				}
			}

			val grid = Array2D(charGrid.xSize, charGrid.ySize) { x, y -> Tile(x, y) }

			for (x in 0 until charGrid.xSize)
			{
				for (y in 0 until charGrid.ySize)
				{
					val tile = grid[x, y]
					val char = charGrid[x, charGrid.ySize - 1 - y]

					loadTile(tile, char)
				}
			}

			val level = Level(grid)
			for (tile in grid)
			{
				tile.level = level
			}

			level.ambient.set(AssetManager.loadColour(xml.getChildByName("Ambient")!!))

			return level
		}
	}
}