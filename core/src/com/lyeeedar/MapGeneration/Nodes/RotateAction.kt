package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData

class RotateAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var degrees: CompiledExpression

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seed = generator.ran.nextLong()

		val angle = degrees.evaluate(variables, seed)
		args.area.orientation += angle
	}

	override fun parse(xmlData: XmlData)
	{
		degrees = CompiledExpression(xmlData.get("Degrees").toLowerCase(), Area.defaultVariables)
	}

	override fun resolve()
	{

	}
}