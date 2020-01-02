package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.LanesMapGenerator
import squidpony.squidmath.RNG

class SquidlibLanesMapGeneratorAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var numLanes: Int = 1

	var numCaveCarvers = 0
	var numBoxRoomCarvers = 0
	var numWalledBoxRoomCarvers = 0
	var numRoundRoomCarvers = 0
	var numWalledRoundRoomCarvers = 0

	var overwrite = true

	override fun execute(args: NodeArguments)
	{
		val gen = LanesMapGenerator(args.area.width, args.area.height, RNG(generator.ran), numLanes)
		gen.putCaveCarvers(numCaveCarvers)
		gen.putBoxRoomCarvers(numBoxRoomCarvers)
		gen.putWalledBoxRoomCarvers(numWalledBoxRoomCarvers)
		gen.putRoundRoomCarvers(numRoundRoomCarvers)
		gen.putWalledRoundRoomCarvers(numWalledRoundRoomCarvers)
		gen.generate()

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
	}

	override fun parse(xmlData: XmlData)
	{
		numLanes = xmlData.getInt("NumLanes", 1)
		numCaveCarvers = xmlData.getInt("NumCaveCarvers", 0)
		numBoxRoomCarvers = xmlData.getInt("NumBoxRoomCarvers", 0)
		numWalledBoxRoomCarvers = xmlData.getInt("NumWalledBoxRoomCarvers", 0)
		numRoundRoomCarvers = xmlData.getInt("NumRoundRoomCarvers", 0)
		numWalledRoundRoomCarvers = xmlData.getInt("NumWalledRoundRoomCarvers", 0)
		overwrite = xmlData.getBoolean("Overwrite", true)
	}

	override fun resolve()
	{

	}
}