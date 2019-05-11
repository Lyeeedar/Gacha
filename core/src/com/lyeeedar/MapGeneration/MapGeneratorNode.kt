package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.lyeeedar.MapGeneration.Nodes.AbstractMapGenerationAction
import com.lyeeedar.MapGeneration.Nodes.NodeArguments
import com.lyeeedar.Util.XmlData

class MapGeneratorNode
{
	val actions = Array<AbstractMapGenerationAction>()

	fun execute(args: NodeArguments)
	{
		for (action in actions)
		{
			action.execute(args)
		}
	}

	fun parse(xmlData: XmlData, generator: MapGenerator)
	{
		for (el in xmlData.children)
		{
			 val action = AbstractMapGenerationAction.load(el, generator)
			actions.add(action)
		}
	}

	fun resolve()
	{
		for (action in actions)
		{
			action.resolve()
		}
	}
}