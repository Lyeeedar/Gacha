package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData

class PerPointAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var nodeGuid: String
	lateinit var node: MapGeneratorNode

	override fun execute(args: NodeArguments)
	{
		for (point in args.area.getAllPoints())
		{
			val newArea = args.area.copy()
			newArea.points.clear()
			newArea.points.add(point)

			val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)

			node.execute(newArgs)
		}
	}

	override fun parse(xmlData: XmlData)
	{
		nodeGuid = xmlData.get("Node")
	}

	override fun resolve()
	{
		node = generator.nodeMap[nodeGuid]
	}
}