package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.floor

class RepeatAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class RemainderMode
	{
		NODE,
		PAD,
		EXPAND
	}

	lateinit var remainderMode: RemainderMode
	var onX = true
	lateinit var size: CompiledExpression

	lateinit var remainderGUID: String
	lateinit var childGUID: String

	lateinit var child: MapGeneratorNode
	var remainder: MapGeneratorNode? = null

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		args.area.xMode = onX

		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seed = generator.ran.nextLong()

		val points = Array<RepeatDivision>()

		var current = 0
		val totalSize = args.area.size

		while (current < totalSize)
		{
			val size = size.evaluate(variables, seed).floor()

			if (current + size > totalSize) break

			points.add(RepeatDivision(current, size))

			current += size
		}

		val remaining = totalSize - current
		if (remaining > 0)
		{
			if (remainderMode == RemainderMode.PAD)
			{
				val paddingRaw = remaining.toFloat() / points.size.toFloat()
				val padding = paddingRaw.toInt()
				val paddingRemainder = paddingRaw - padding

				var accumulatedOffset = 0
				var accumulatedRemainder = 0f
				for (point in points)
				{
					point.pos += accumulatedOffset
					accumulatedOffset += padding

					accumulatedRemainder += paddingRemainder
					if (accumulatedRemainder > 1f)
					{
						accumulatedRemainder -= 1f

						accumulatedOffset += 1
					}
				}
			}
			else if (remainderMode == RemainderMode.EXPAND)
			{
				val paddingRaw = remaining.toFloat() / points.size.toFloat()
				val padding = paddingRaw.toInt()
				val paddingRemainder = paddingRaw - padding

				var accumulatedOffset = 0
				var accumulatedRemainder = 0f
				for (point in points)
				{
					point.pos += accumulatedOffset
					point.size += padding
					accumulatedOffset += padding

					accumulatedRemainder += paddingRemainder
					if (accumulatedRemainder > 1f)
					{
						accumulatedRemainder -= 1f

						point.size += 1
						accumulatedOffset += 1
					}
				}
			}
		}

		for (point in points)
		{
			val newArea = args.area.copy()
			newArea.pos = args.area.pos + point.pos
			newArea.size = point.size

			newArea.points.clear()
			newArea.addPointsWithin(args.area)

			if (newArea.hasContents)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				child.execute(newArgs)
			}
		}

		if (remainderMode == RemainderMode.NODE && remainder != null && remaining > 0)
		{
			val newArea = args.area.copy()
			newArea.pos = args.area.pos + current
			newArea.size = remaining

			newArea.points.clear()
			newArea.addPointsWithin(args.area)

			if (newArea.hasContents)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				remainder!!.execute(newArgs)
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		onX = xmlData.get("Axis", "X") == "X"
		size = CompiledExpression(xmlData.get("Size").replace("%", "#size").toLowerCase(), Area.defaultVariables)
		childGUID = xmlData.get("Node")
		remainderGUID = xmlData.get("Remainder", "")!!
		remainderMode = RemainderMode.valueOf(xmlData.get("RemainderMode", "Node")!!.toUpperCase())
	}

	override fun resolve()
	{
		child = generator.nodeMap[childGUID]

		if (remainderGUID.isNotBlank())
		{
			remainder = generator.nodeMap[remainderGUID]
		}
	}
}

class RepeatDivision(var pos: Int, var size: Int)