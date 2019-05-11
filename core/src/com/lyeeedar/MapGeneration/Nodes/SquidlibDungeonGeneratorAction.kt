package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonGenerator
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG

class SquidlibDungeonGeneratorAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var tilesetType: TilesetType
	var water: Int = 0
	var grass: Int = 0
	var traps: Int = 0
	var doors: Int = 0
	var startChar: Char = ' '
	var endChar: Char = ' '
	var overwrite = true

	override fun execute(args: NodeArguments)
	{
		val gen = DungeonGenerator(args.area.width, args.area.height, RNG(generator.ran))
		gen.addWater(water)
		gen.addGrass(grass)
		gen.addTraps(traps)
		gen.addDoors(doors, true)
		gen.generate(tilesetType)

		val map = gen.dungeon
		DungeonUtility.closeDoors(map)

		if (map.size != args.area.width || map[0].size != args.area.height) throw Exception("Generator map is the wrong size!")

		for (x in 0 until args.area.width)
		{
			for (y in 0 until args.area.height)
			{
				val char = map[x][y]
				val symbolToWrite = args.symbolTable[char]!!

				val symbol = args.area[x, y] ?: continue
				symbol.write(symbolToWrite, overwrite)
			}
		}

		if (startChar != ' ')
		{
			val symbolToWrite = args.symbolTable[startChar]!!

			val symbol = args.area[gen.stairsUp.x, gen.stairsUp.y]

			if (symbol != null)
			{
				symbol.write(symbolToWrite, overwrite)
			}
		}

		if (endChar != ' ')
		{
			val symbolToWrite = args.symbolTable[endChar]!!

			val symbol = args.area[gen.stairsDown.x, gen.stairsDown.y]

			if (symbol != null)
			{
				symbol.write(symbolToWrite, overwrite)
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		tilesetType = TilesetType.valueOf(xmlData.get("TilesetType").toUpperCase())
		water = xmlData.getInt("PercentWater", 0)
		grass = xmlData.getInt("PercentGrass", 0)
		traps = xmlData.getInt("PercentTraps", 0)
		doors = xmlData.getInt("PercentDoors", 0)
		startChar = xmlData.get("StartChar", " ")!![0]
		endChar = xmlData.get("EndChar", " ")!![0]
		overwrite = xmlData.getBoolean("Overwrite", true)
	}

	override fun resolve()
	{

	}
}