package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.exp4j.Helpers.unescapeCharacters
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData

class ConditionAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	val conditions = Array<Condition>()

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		for (cond in conditions)
		{
			var execute = false
			if (cond.condition == null)
			{
				execute = true
			}
			else
			{
				variables.clear()
				variables.putAll(args.variables)
				args.area.writeVariables(variables)

				val seed = generator.ran.nextLong()
				execute = cond.condition.evaluate(variables, seed) > 0
			}

			if (execute)
			{
				cond.child?.execute(args)
				break
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children())
		{
			val condition = el.get("Condition").toLowerCase().replace("%", "#size").unescapeCharacters()
			val node = el.get("Node", "")!!

			val compiled = if (condition == "else") null else CompiledExpression(condition, Area.defaultVariables)

			conditions.add(Condition(compiled, node))
		}
	}

	override fun resolve()
	{
		for (cond in conditions)
		{
			if (cond.childGUID.isNotBlank())
			{
				cond.child = generator.nodeMap[cond.childGUID]
			}
		}
	}
}

class Condition(val condition: CompiledExpression?, val childGUID: String)
{
	var child: MapGeneratorNode? = null
}