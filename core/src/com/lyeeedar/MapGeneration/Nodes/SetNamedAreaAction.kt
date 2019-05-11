package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class SetNamedAreaAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var name: String
	var overwrite = false

	override fun execute(args: NodeArguments)
	{
		val newArea = args.area.copy()

		var areas = generator.namedAreas[name]
		if (areas == null)
		{
			areas = Array()
			generator.namedAreas[name] = areas
		}

		if (overwrite)
		{
			areas.clear()
		}

		areas.add(newArea)
	}

	override fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name")
		overwrite = xmlData.getBoolean("Overwrite", false)
	}

	override fun resolve()
	{

	}
}