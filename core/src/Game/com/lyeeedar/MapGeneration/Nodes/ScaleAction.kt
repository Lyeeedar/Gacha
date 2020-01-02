package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Direction
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData

class ScaleAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class Mode
	{
		ADDITIVE,
		MULTIPLICATIVE,
		ABSOLUTE
	}

	lateinit var mode: Mode
	lateinit var xEqn: CompiledExpression
	lateinit var yEqn: CompiledExpression
	lateinit var snap: Direction

	val variables = ObjectFloatMap<String>()
	override fun execute(args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seedX = generator.ran.nextLong()
		val seedY = generator.ran.nextLong()

		val oldWidth = args.area.width
		val oldHeight = args.area.height

		val x = xEqn.evaluate(variables, seedX)
		val y = yEqn.evaluate(variables, seedY)

		if (mode == Mode.ADDITIVE)
		{
			args.area.width += x.toInt()
			args.area.height += y.toInt()
		}
		else if (mode == Mode.MULTIPLICATIVE)
		{
			args.area.width = (args.area.width * x).toInt()
			args.area.height = (args.area.height * y).toInt()
		}
		else if (mode == Mode.ABSOLUTE)
		{
			args.area.width = x.toInt()
			args.area.height = y.toInt()
		}
		else throw Exception("Unhandled scale mode '$mode'!")

		val diffX = args.area.width - oldWidth
		val diffY = args.area.height - oldHeight

		if (snap.x == 0)
		{
			args.area.x -= diffX / 2
		}
		else if (snap.x < 0)
		{

		}
		else if (snap.x > 0)
		{
			args.area.x = (args.area.x + oldWidth) - args.area.width
		}

		if (snap.y == 0)
		{
			args.area.y -= diffY / 2
		}
		else if (snap.y < 0)
		{

		}
		else if (snap.y > 0)
		{
			args.area.y = (args.area.y + oldHeight) - args.area.height
		}

		if (args.area.isPoints)
		{
			for (point in args.area.points.toList())
			{
				if (point.x < args.area.x || point.y < args.area.y || point.x >= args.area.x+args.area.width || point.y >= args.area.y+args.area.height)
				{
					args.area.points.removeValue(point, true)
				}
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		mode = Mode.valueOf(xmlData.get("Mode", "Additive")!!.toUpperCase())
		xEqn = CompiledExpression(xmlData.get("X").toLowerCase().replace("%", "#size"), Area.defaultVariables)
		yEqn = CompiledExpression(xmlData.get("Y").toLowerCase().replace("%", "#size"), Area.defaultVariables)
		snap = Direction.valueOf(xmlData.get("Snap", "Center")!!.toUpperCase())
	}

	override fun resolve()
	{

	}
}