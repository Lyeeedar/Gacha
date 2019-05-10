package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.floor

class TranslateAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class Mode
	{
		RELATIVE,
		ABSOLUTE
	}

	lateinit var xEqn: CompiledExpression
	lateinit var yEqn: CompiledExpression
	lateinit var mode: Mode

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seedX = generator.ran.nextLong()
		val seedY = generator.ran.nextLong()

		val x = xEqn.evaluate(variables, seedX).floor()
		val y = yEqn.evaluate(variables, seedY).floor()

		if (mode == Mode.RELATIVE)
		{
			args.area.x += x
			args.area.y += y

			if (args.area.isPoints)
			{
				args.area.points.forEachIndexed { i, pos -> args.area.points[i] = Pos(pos.x + x, pos.y + y) }
			}
		}
		else if (mode == Mode.ABSOLUTE)
		{
			val dx = x - args.area.x
			val dy = y - args.area.y

			args.area.x += dx
			args.area.y += dy

			if (args.area.isPoints)
			{
				args.area.points.forEachIndexed { i, pos -> args.area.points[i] = Pos(pos.x + dx, pos.y + dy) }
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		xEqn = CompiledExpression(xmlData.get("X", "0")!!.toLowerCase().replace("%", "#size"), Area.defaultVariables)
		yEqn = CompiledExpression(xmlData.get("Y", "0")!!.toLowerCase().replace("%", "#size"), Area.defaultVariables)
		mode = Mode.valueOf(xmlData.get("Mode", "Relative")!!.toUpperCase())
	}

	override fun resolve()
	{

	}
}