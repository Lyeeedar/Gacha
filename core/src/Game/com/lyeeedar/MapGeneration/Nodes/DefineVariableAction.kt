package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.set

class DefineVariableAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var key: String
	lateinit var valueExp: CompiledExpression

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)

		val seed = generator.ran.nextLong()
		val value = valueExp.evaluate(variables, seed)

		args.variables[key] = value
	}

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key").toLowerCase()
		valueExp = CompiledExpression(xmlData.get("Value"), Area.defaultVariables)

		when (key)
		{
			"size", "pos", "count", "x", "y", "width", "height" -> throw UnsupportedOperationException("Define is using reserved name '$key'!")
		}
	}

	override fun resolve()
	{

	}
}