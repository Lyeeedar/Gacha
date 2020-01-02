package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.Util.*

class SelectNamedAreaAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class Mode
	{
		RANDOM,
		SMALLEST,
		LARGEST,
		CLOSEST,
		FURTHEST
	}

	lateinit var mode: Mode
	lateinit var countExp: CompiledExpression
	lateinit var name: String

	lateinit var nodeGuid: String
	lateinit var remainderGuid: String

	var node: MapGeneratorNode? = null
	var remainder: MapGeneratorNode? = null

	val tempArray = Array<Area>(false, 8)
	val variables = com.badlogic.gdx.utils.ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		val areas = generator.namedAreas[name]!!

		variables.clear()
		variables.putAll(args.variables)
		variables["count"] = areas.size.toFloat()
		val seed = generator.ran.nextLong()

		val count = countExp.evaluate(variables, seed).floor()

		tempArray.addAll(areas)

		when (mode)
		{
			Mode.RANDOM -> {
				for (i in 0 until count)
				{
					val area = tempArray.removeRandom(generator.ran)
					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.SMALLEST -> {
				val sorted = tempArray.sortedBy { it.getAllPoints().size }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.LARGEST -> {
				val sorted = tempArray.sortedByDescending { it.getAllPoints().size }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.CLOSEST -> {
				val sorted = tempArray.sortedBy { it.getAllPoints().map<Pos, Float>{ ap -> args.area.getAllPoints().map<Pos, Float>{ tp -> tp.dst2(ap) }.min()!! }.min()!! }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.FURTHEST -> {
				val sorted = tempArray.sortedByDescending { it.getAllPoints().map<Pos, Float>{ ap -> args.area.getAllPoints().map<Pos, Float>{ tp -> tp.dst2(ap) }.min()!! }.min()!! }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(newArgs)

					if (tempArray.size == 0) break
				}
			}
		}

		if (remainder != null)
		{
			for (area in tempArray)
			{
				val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)
				remainder!!.execute(newArgs)
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		mode = Mode.valueOf(xmlData.get("Mode", "Random")!!.toUpperCase())
		countExp = CompiledExpression(xmlData.get("Count", "1")!!.toLowerCase().replace("%", "#count"), ObjectFloatMap(mapOf(Pair("count", 0f))))
		name = xmlData.get("Name")

		nodeGuid = xmlData.get("Node", "")!!
		remainderGuid = xmlData.get("Remainder", "")!!
	}

	override fun resolve()
	{
		if (nodeGuid.isNotBlank()) node = generator.nodeMap[nodeGuid]
		if (remainderGuid.isNotBlank()) remainder = generator.nodeMap[remainderGuid]
	}
}