package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.FlowingCaveGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG

class SquidlibFlowingCaveGeneratorAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var tilesetType: TilesetType
	var roomChance = 0.0
	var overwrite = true

	override fun execute(args: NodeArguments)
	{
		val gen = FlowingCaveGenerator(args.area.width, args.area.height, tilesetType, RNG(generator.ran))
		gen.generate(tilesetType, roomChance)

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
		tilesetType = TilesetType.valueOf(xmlData.get("TilesetType").toUpperCase())
		roomChance = xmlData.getFloat("RoomChance", 0f).toDouble()
		overwrite = xmlData.getBoolean("Overwrite", true)
	}

	override fun resolve()
	{

	}
}