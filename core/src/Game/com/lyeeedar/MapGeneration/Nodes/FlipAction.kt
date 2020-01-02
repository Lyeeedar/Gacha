package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData

class FlipAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	var onX = true

	override fun execute(args: NodeArguments)
	{
		if (onX)
		{
			args.area.flipX = !args.area.flipX
		}
		else
		{
			args.area.flipY = !args.area.flipY
		}
	}

	override fun parse(xmlData: XmlData)
	{
		onX = xmlData.get("Axis", "X") == "X"
	}

	override fun resolve()
	{

	}
}
