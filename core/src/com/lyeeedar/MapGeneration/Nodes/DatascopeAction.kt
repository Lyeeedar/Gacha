package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData

class DatascopeAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var scopeVariables = true
	var scopeSymbols = true
	var scopeArea = true

	lateinit var childGUID: String
	lateinit var child: MapGeneratorNode

	override fun execute(args: NodeArguments)
	{
		val cpy = args.copy(scopeArea, scopeVariables, scopeSymbols)
		child.execute(cpy)
	}

	override fun parse(xmlData: XmlData)
	{
		scopeArea = xmlData.getBoolean("Area", true)
		scopeVariables = xmlData.getBoolean("Variables", true)
		scopeSymbols = xmlData.getBoolean("Symbols", true)
		childGUID = xmlData.get("Node")
	}

	override fun resolve()
	{
		child = generator.nodeMap[childGUID]
	}
}