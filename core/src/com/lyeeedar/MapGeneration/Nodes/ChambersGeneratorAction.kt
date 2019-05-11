package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.Direction
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.random
import squidpony.squidmath.LightRNG

class ChambersGeneratorAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var overwrite = true

	// ----------------------------------------------------------------------
	class BSPTree(var x: Int, var y: Int, var width: Int, var height: Int)
	{
		var child1: BSPTree? = null
		var child2: BSPTree? = null
		var splitVertically: Boolean = false

		fun partition(ran: LightRNG)
		{
			if (width < minSize * 2 && height < minSize * 2 || width < maxSize && height < maxSize && ran.nextInt(5) == 0)
			{

			}
			else
			{
				if (width < minSize * 2)
				{
					splitVertically = true
				}
				else if (height < minSize * 2)
				{
					splitVertically = false
				}
				else
				{
					splitVertically = ran.nextBoolean()
				}

				if (splitVertically)
				{
					val split = 0.3f + ran.nextFloat() * 0.3f
					val splitheight = (height * split).toInt()

					child1 = BSPTree(x, y, width, splitheight)
					child2 = BSPTree(x, y + splitheight, width, height - splitheight)

					child1!!.partition(ran)
					child2!!.partition(ran)
				}
				else
				{
					val split = 0.3f + ran.nextFloat() * 0.3f
					val splitwidth = (width * split).toInt()

					child1 = BSPTree(x, y, splitwidth, height)
					child2 = BSPTree(x + splitwidth, y, width - splitwidth, height)

					child1!!.partition(ran)
					child2!!.partition(ran)
				}
			}
		}

		private fun placeDoor(grid: Array2D<Char>, ran: LightRNG)
		{
			val possibleDoorTiles = com.badlogic.gdx.utils.Array<Point>()

			if (splitVertically)
			{
				for (ix in 1 until width)
				{
					val x = this.x + ix
					val y = child2!!.y

					val valid = grid.tryGet(x, y-1, '#') != '#' && grid.tryGet(x, y+1, '#') != '#'
					if (valid)
					{
						possibleDoorTiles.add(Point(x, y))
					}
				}
			}
			else
			{
				for (iy in 1 until height)
				{
					val x = child2!!.x
					val y = this.y + iy

					val valid = grid.tryGet(x-1, y, '#') != '#' && grid.tryGet(x+1, y, '#') != '#'
					if (valid)
					{
						possibleDoorTiles.add(Point(x, y))
					}
				}
			}

			if (possibleDoorTiles.size > 0)
			{
				val doorPos = possibleDoorTiles.random(ran)
				grid[doorPos] = '+'
			}
		}

		fun dig(grid: Array2D<Char>, ran: LightRNG)
		{
			if (child1 != null)
			{
				child1!!.dig(grid, ran)
				child2!!.dig(grid, ran)
				placeDoor(grid, ran)
			}
			else
			{
				for (ix in 1 until width)
				{
					for (iy in 1 until height)
					{
						grid[x + ix, y + iy] = '.'
					}
				}
			}

		}

		companion object
		{

			private val minSize = 4
			private val maxSize = 6
		}
	}

	// ----------------------------------------------------------------------
	override fun execute(args: NodeArguments)
	{
		val grid = Array2D<Char>(args.area.width, args.area.height) { x, y -> '#' }

		while (true)
		{
			val tree = BSPTree(0, 0, grid.width - 1, grid.height - 1)
			tree.partition(generator.ran)
			tree.dig(grid, generator.ran)

			if (isConnected(grid))
			{
				break
			}

			for (x in 0 until grid.width)
			{
				for (y in 0 until grid.height)
				{
					grid[x, y] = '#'
				}
			}

			println("Failed to connect all chambers. Retrying")
		}

		for (x in 0 until args.area.width)
		{
			for (y in 0 until args.area.height)
			{
				val char = grid[x, y]
				val symbolToWrite = args.symbolTable[char]!!

				val symbol = args.area[x, y] ?: continue
				symbol.write(symbolToWrite, overwrite)
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun isConnected(grid: Array2D<Char>): Boolean
	{
		val reached = Array2D<Boolean>(grid.width, grid.height) { x,y -> false }

		var x = 0
		var y = 0

		x = 0
		outer@ while (x < grid.width)
		{
			y = 0
			while (y < grid.height)
			{
				if (grid[x, y] != '#')
				{
					break@outer
				}
				y++
			}
			x++
		}

		val toBeProcessed = com.badlogic.gdx.utils.Array<IntArray>()
		toBeProcessed.add(intArrayOf(x, y))

		while (toBeProcessed.size > 0)
		{
			val point = toBeProcessed.pop()
			x = point[0]
			y = point[1]

			if (reached[x, y])
			{
				continue
			}

			reached[x, y] = true

			for (dir in Direction.Values)
			{
				val nx = x + dir.x
				val ny = y + dir.y

				if (grid.inBounds(nx, ny) && grid[nx, ny] != '#')
				{
					toBeProcessed.add(intArrayOf(nx, ny))
				}
			}
		}

		x = 0
		while (x < grid.width)
		{
			y = 0
			while (y < grid.height)
			{
				if (grid[x, y] != '#' && !reached[x, y])
				{
					return false
				}
				y++
			}
			x++
		}

		return true
	}

	override fun parse(xmlData: XmlData)
	{
		overwrite = xmlData.getBoolean("Overwrite", true)
	}

	override fun resolve()
	{

	}
}