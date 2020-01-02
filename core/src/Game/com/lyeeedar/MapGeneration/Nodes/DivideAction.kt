package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.floor

class DivideAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	val divisions = Array<Division>()
	var onX = true

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		args.area.xMode = onX

		var current = 0
		for (division in divisions)
		{
			// setup variables
			variables.clear()
			variables.putAll(args.variables)
			args.area.writeVariables(variables)
			val seed = generator.ran.nextLong()

			// evaluate size
			val size = if (division.size == null) args.area.size - current else Math.min(args.area.size - current, division.size.evaluate(variables, seed).floor())

			if (division.child != null && size > 0)
			{
				val newArea = args.area.copy()
				newArea.pos = args.area.pos + current
				newArea.size = size
				newArea.points.clear()
				newArea.addPointsWithin(args.area)

				if (newArea.hasContents)
				{
					val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
					division.child!!.execute(newArgs)
				}
			}

			current += size
			if (current == args.area.size) break
		}
	}

	override fun parse(xmlData: XmlData)
	{
		onX = xmlData.getAttribute("Axis", "X") == "X"

		for (el in xmlData.children())
		{
			val size = el.get("Size").toLowerCase().replace("%", "#size")
			val child = el.get("Node", "")!!
			val exp = if (size == "remainder") null else CompiledExpression(size, Area.defaultVariables)

			divisions.add(Division(exp, child))
		}
	}

	override fun resolve()
	{
		for (division in divisions)
		{
			if (division.childGUID.isNotBlank())
			{
				division.child = generator.nodeMap[division.childGUID]
			}
		}
	}
}

class Division(val size: CompiledExpression?, val childGUID: String)
{
	var child: MapGeneratorNode? = null
}