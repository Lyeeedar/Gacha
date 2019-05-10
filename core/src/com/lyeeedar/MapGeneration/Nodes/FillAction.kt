package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData

class FillAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var char: Char = ' '
	var overwrite = false

	override fun execute(args: NodeArguments)
	{
		val symbolToWrite = args.symbolTable[char]

		for (pos in args.area.getAllPoints())
		{
			val symbol = args.area[pos.x - args.area.x, pos.y - args.area.y] ?: continue

			symbol.write(symbolToWrite, overwrite)
		}
	}

	override fun parse(xmlData: XmlData)
	{
		char = xmlData.get("Character")[0]
		overwrite = xmlData.getBoolean("Overwrite", false)
	}

	override fun resolve()
	{

	}

}