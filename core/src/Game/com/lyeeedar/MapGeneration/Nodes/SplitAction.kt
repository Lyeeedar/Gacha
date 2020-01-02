package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.floor
import com.lyeeedar.Util.round

class SplitAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class SplitSide
	{
		NORTH,
		SOUTH,
		EAST,
		WEST,
		EDGE,
		REMAINDER
	}

	class Split(val side: SplitSide, val size: CompiledExpression, val childGUID: String)
	{
		var child: MapGeneratorNode? = null
	}

	val splits = Array<Split>()

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		var currentArea = args.area.copy()
		for (i in 0 until splits.size)
		{
			val split = splits[i]

			val newArea = currentArea.copy()
			val nextArea = currentArea.copy()

			if (split.side == SplitSide.NORTH)
			{
				currentArea.xMode = false

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).floor()

				newArea.x = currentArea.x
				newArea.y = currentArea.y + currentArea.height - size
				newArea.width = currentArea.width
				newArea.height = size
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width
				nextArea.height = currentArea.height - size
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.SOUTH)
			{
				currentArea.xMode = false

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x
				newArea.y = currentArea.y
				newArea.width = currentArea.width
				newArea.height = size
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y + size
				nextArea.width = currentArea.width
				nextArea.height = currentArea.height - size
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.WEST)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x
				newArea.y = currentArea.y
				newArea.width = size
				newArea.height = currentArea.height
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x + size
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width - size
				nextArea.height = currentArea.height
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.EAST)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x + currentArea.width - size
				newArea.y = currentArea.y
				newArea.width = size
				newArea.height = currentArea.height
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width - size
				nextArea.height = currentArea.height
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.EDGE)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				if (!newArea.isPoints)
				{
					newArea.points.clear()
					newArea.convertToPoints()
				}

				nextArea.x = currentArea.x + size
				nextArea.y = currentArea.y + size
				nextArea.width = currentArea.width - size * 2
				nextArea.height = currentArea.height - size * 2
				nextArea.points.clear()
				nextArea.addPointsWithin(newArea)

				for (point in nextArea.points) newArea.points.removeValue(point, true)
			}
			else if (split.side == SplitSide.REMAINDER)
			{
				if (split.child != null)
				{
					val newArgs = NodeArguments(currentArea, args.variables, args.symbolTable)
					split.child!!.execute(newArgs)
				}

				break
			}
			else throw Exception("Unhandled split side '" + split.side + "'!")

			currentArea = nextArea

			if (split.child != null)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				split.child!!.execute(newArgs)
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children())
		{
			val side = SplitSide.valueOf(el.get("Side", "North")!!.toUpperCase())
			val size = CompiledExpression(el.get("Size", "1")!!.toLowerCase().replace("%", "#size"), Area.defaultVariables)
			val child = el.get("Node")

			splits.add(Split(side, size, child))
		}
	}

	override fun resolve()
	{
		for (split in splits)
		{
			if (split.childGUID.isNotBlank())
			{
				split.child = generator.nodeMap[split.childGUID]
			}
		}
	}
}