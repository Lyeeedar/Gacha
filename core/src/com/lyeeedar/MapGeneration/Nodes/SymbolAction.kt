package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class SymbolAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var symbolDef: XmlData

	override fun execute(args: NodeArguments)
	{
		val symbol = Symbol.load(symbolDef, args.symbolTable)
		args.symbolTable[symbol.char] = symbol
	}

	override fun parse(xmlData: XmlData)
	{
		symbolDef = xmlData.getChildByName("Symbol")!!
	}

	override fun resolve()
	{

	}
}