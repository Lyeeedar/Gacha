package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Pathfinding.AStarPathfind
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.random

class ConnectRoomsAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class PathStyle
	{
		STRAIGHT,
		WANDERING
	}

	lateinit var pathStyle: PathStyle
	var width = 1
	lateinit var roomName: String

	var central: CorridorFeature? = null
	var side: CorridorFeature? = null

	override fun execute(args: NodeArguments)
	{
		val rooms = generator.namedAreas[roomName]!!

		val connected = BooleanArray(rooms.size)
		val points = FloatArray(rooms.size * 2)
		for (i in 0 until rooms.size)
		{
			val room = rooms[i]
			val clearTile = room.getAllPoints().filter { room[it]!!.type == Symbol.SymbolType.FLOOR }.random(generator.ran)
			val world = room.localToWorld(clearTile)
			points[(i*2)+0] = world.x.toFloat()
			points[(i*2)+1] = world.y.toFloat()
		}

		val triangulator = DelaunayTriangulator()
		val tris = triangulator.computeTriangles(points, false)

		val addedSet = ObjectSet<String>()
		for (i in 0 until tris.size / 3)
		{
			val t1 = tris[(i*3)+0].toInt()
			val t2 = tris[(i*3)+1].toInt()
			val t3 = tris[(i*3)+2].toInt()

			val vectors = arrayOf(Vector2(points[t1], points[t1+1]), Vector2(points[t2], points[t2+1]), Vector2(points[t3], points[t3+1]))
			val sorted = vectors.sortedBy { it.x * 100 + it.y }

			val p1 = sorted[0]
			val p2 = sorted[1]
			val p3 = sorted[2]

			val p1p2 = p1.dst2(p2)
			val p1p3 = p1.dst2(p3)
			val p2p3 = p2.dst2(p3)

			var start: Vector2
			var end: Vector2
			if (p1p2 < p1p3 && p1p2 < p2p3)
			{
				start = p1
				end = p2

				connected[t1/2] = true
				connected[t2/2] = true
			}
			else if (p1p3 < p2p3)
			{
				start = p1
				end = p3

				connected[t1/2] = true
				connected[t3/2] = true
			}
			else
			{
				start = p2
				end = p3

				connected[t2/2] = true
				connected[t3/2] = true
			}

			val key = start.toString() + end.toString()
			if (!addedSet.contains(key))
			{
				addedSet.add(key)

				carveCorridor(start, end, args)
			}
 		}

		// connect unconnected rooms
		for (i in 0 until rooms.size)
		{
			if (!connected[i])
			{
				val thisPos = Vector2(points[(i*2)+0], points[(i*2)+1])

				// find closest connected one, path to it
				var minDst = Float.MAX_VALUE
				val minPos = Vector2()
				val tempPos = Vector2()
				var foundPath = false

				for (i in 0 until rooms.size)
				{
					if (connected[i])
					{
						tempPos.set(Vector2(points[(i * 2) + 0], points[(i * 2) + 1]))
						val dst = tempPos.dst2(thisPos)
						if (dst < minDst)
						{
							minDst = dst
							minPos.set(tempPos)
						}

						foundPath = true
					}
				}

				connected[i] = true

				if (foundPath)
				{
					carveCorridor(thisPos, minPos, args)
				}
			}
		}
	}

	fun carveCorridor(start: Vector2, end: Vector2, args: NodeArguments)
	{
		val x1 = start.x.toInt()
		val y1 = start.y.toInt()
		val x2 = end.x.toInt()
		val y2 = end.y.toInt()

		val pathfinder = AStarPathfind(args.area.grid, x1, y1, x2, y2, true, width, SpaceSlot.ENTITY, this)
		val path = pathfinder.path

		if (path == null)
		{
			// :(
		}
		else
		{
			doCarveCorridor(path, args)
		}
	}

	fun doCarveCorridor(path: Array<Point>, args: NodeArguments)
	{
		var centralCount = 0
		var sideCount = 0
		var placementAlternator = true

		val tiles = args.area.grid

		for (i in 0 until path.size)
		{
			val pos = path[i]

			var t: Symbol

			for (x in 0 until width)
			{
				for (y in 0 until width)
				{
					t = tiles[pos.x + x, pos.y + y]

					if (!t.locked)
					{
						if (t.char == '#')
						{
							t.write(args.symbolTable['.'], true)
						}

						// Wipe out all features not placed by this path
						if (t.placerHashCode != -1 && t.placerHashCode != path.hashCode())
						{
							t.content = null
						}

						// Wipe out all features in the central square
						if (x > 0 && x < width - 1 && y > 0 && y < width - 1)
						{
							t.content = null
						}
					}
				}
			}

			if (central != null)
			{
				centralCount++
				if (centralCount >= central!!.interval)
				{
					t = tiles[pos.x + width / 2, pos.y + width / 2]

					if (!t.locked)
					{
						t.write(args.symbolTable[central!!.char], true)
						t.placerHashCode = path.hashCode()
					}

					centralCount = 0
				}
			}

			if (side != null)
			{
				val side = side!!

				sideCount++

				if (sideCount >= side.interval && i > 0)
				{
					val placeTop = (side.placementMode == CorridorFeature.PlacementMode.BOTH
									|| side.placementMode == CorridorFeature.PlacementMode.TOP
									|| side.placementMode == CorridorFeature.PlacementMode.ALTERNATE && placementAlternator)

					val placeBottom = (side.placementMode == CorridorFeature.PlacementMode.BOTH
									   || side.placementMode == CorridorFeature.PlacementMode.BOTTOM
									   || side.placementMode == CorridorFeature.PlacementMode.ALTERNATE && !placementAlternator)

					if (path.get(i - 1).x != pos.x)
					{
						if (width == 1)
						{
							if (placeTop && isEmpty(tiles[pos.x + width / 2, pos.y - 1]))
							{
								t = tiles[pos.x + width / 2, pos.y - 1]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.NORTH
									t.placerHashCode = path.hashCode()
								}
							}

							if (placeBottom && isEmpty(tiles[pos.x + width / 2, pos.y + width]))
							{
								t = tiles[pos.x + width / 2, pos.y + width]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.SOUTH
									t.placerHashCode = path.hashCode()
								}
							}
						}
						else
						{
							if (placeTop && tiles[pos.x + width / 2, pos.y - 1].char == '#')
							{
								t = tiles[pos.x + width / 2, pos.y]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.NORTH
									t.placerHashCode = path.hashCode()
								}
							}

							if (placeBottom && tiles[pos.x + width / 2, pos.y + width].char == '#')
							{
								t = tiles[pos.x + width / 2, pos.y + width - 1]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.SOUTH
									t.placerHashCode = path.hashCode()
								}
							}
						}
					}
					else
					{
						if (width == 1)
						{
							if (placeTop && isEmpty(tiles[pos.x - 1, pos.y + width / 2]))
							{
								t = tiles[pos.x - 1, pos.y + width / 2]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.EAST
									t.placerHashCode = path.hashCode()
								}
							}

							if (placeBottom && isEmpty(tiles[pos.x + width, pos.y + width / 2]))
							{
								t = tiles[pos.x + width, pos.y + width / 2]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.WEST
									t.placerHashCode = path.hashCode()
								}
							}
						}
						else
						{
							if (placeTop && tiles[pos.x - 1, pos.y + width / 2].char == '#')
							{
								t = tiles[pos.x, pos.y + width / 2]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.EAST
									t.placerHashCode = path.hashCode()
								}
							}

							if (placeBottom && tiles[pos.x + width, pos.y + width / 2].char == '#')
							{
								t = tiles[pos.x + width - 1, pos.y + width / 2]

								if (!t.locked)
								{
									t.write(args.symbolTable[side.char], false)
									//t.attachLocation = Direction.WEST
									t.placerHashCode = path.hashCode()
								}
							}
						}
					}

					sideCount = 0
					placementAlternator = !placementAlternator
				}
			}
		}
	}

	fun isEmpty(symbol: Symbol) = symbol.placerHashCode == -1 && symbol.content == null && symbol.char == '.'

	override fun parse(xmlData: XmlData)
	{
		pathStyle = PathStyle.valueOf(xmlData.get("PathStyle", "Straight")!!.toUpperCase())
		width = xmlData.getInt("Width", 1)
		roomName = xmlData.get("RoomName")

		val centralEl = xmlData.getChildByName("Central")
		if (centralEl != null)
		{
			central = CorridorFeature()
			central?.parse(centralEl)
		}

		val sideEl = xmlData.getChildByName("Side")
		if (sideEl != null)
		{
			side = CorridorFeature()
			side?.parse(sideEl)
		}
	}

	override fun resolve()
	{

	}
}

class CorridorFeature
{
	enum class PlacementMode
	{
		BOTH,
		TOP,
		BOTTOM,
		ALTERNATE
	}

	lateinit var placementMode: PlacementMode
	var interval = 0
	var char = ' '

	fun parse(xmlData: XmlData)
	{
		placementMode = PlacementMode.valueOf(xmlData.get("PlacementMode", "Both")!!.toUpperCase())
		interval = xmlData.getInt("Interval", 0)
		char = xmlData.get("Character")[0]
	}
}