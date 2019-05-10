package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData

class NodeAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var childGUID: String
	lateinit var child: MapGeneratorNode

	override fun execute(args: NodeArguments)
	{
		child.execute(args)
	}

	override fun parse(xmlData: XmlData)
	{
		childGUID = xmlData.get("Node")
	}

	override fun resolve()
	{
		child = generator.nodeMap[childGUID]
	}
}