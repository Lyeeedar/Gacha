package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.SymmetryDungeonGenerator
import squidpony.squidmath.RNG

class SquidlibSymmetryGeneratorAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var overwrite = true

	override fun execute(args: NodeArguments)
	{
		val gen = SymmetryDungeonGenerator(args.area.width, args.area.height, RNG(generator.ran))
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
		overwrite = xmlData.getBoolean("Overwrite", true)
	}

	override fun resolve()
	{

	}
}