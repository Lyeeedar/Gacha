package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData

class DeferAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var childGUID: String
	lateinit var child: MapGeneratorNode

	override fun execute(args: NodeArguments)
	{
		generator.deferredNodes.add(DeferredNode(child, args.copy(true, true, true)))
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

class DeferredNode(val node: MapGeneratorNode, val args: NodeArguments)