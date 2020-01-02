package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Nodes.DeferredNode
import com.lyeeedar.MapGeneration.Nodes.NodeArguments
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set
import squidpony.squidmath.LightRNG

class MapGenerator
{
	var baseSize = Point(0, 0)

	lateinit var ran: LightRNG
	val nodeMap = ObjectMap<String, MapGeneratorNode>()
	lateinit var root: MapGeneratorNode

	val deferredNodes = Array<DeferredNode>()
	val namedAreas = ObjectMap<String, Array<Area>>()

	private val executingArray = Array<DeferredNode>()
	fun execute(seed: Long)
	{
		ran = LightRNG(seed)

		val grid = Array2D<Symbol>(baseSize.x, baseSize.y) { x,y -> Symbol(' ') }
		val area = Area(baseSize.x, baseSize.y, grid)
		val args = NodeArguments(area, ObjectFloatMap(), ObjectMap())

		deferredNodes.add(DeferredNode(root, args))
		while (deferredNodes.size > 0)
		{
			executingArray.clear()
			executingArray.addAll(deferredNodes)
			deferredNodes.clear()

			for (i in 0 until executingArray.size)
			{
				val node = executingArray[i]
				node.node.execute(node.args)
			}
		}

		for (y in 0 until baseSize.y)
		{
			for (x in 0 until baseSize.x)
			{
				print(grid[x, y].char)
			}
			print("\n")
		}
	}

	fun parse(xmlData: XmlData)
	{
		baseSize.parse(xmlData.get("Size"))
		val rootGuid = xmlData.get("Root")

		val nodesEl = xmlData.getChildByName("Nodes")!!
		for (nodeEl in nodesEl.children)
		{
			val guid = nodeEl.getAttribute("GUID")

			val node = MapGeneratorNode()
			node.parse(nodeEl, this)

			nodeMap[guid] = node
		}

		for (node in nodeMap.values())
		{
			node.resolve()
		}

		root = nodeMap[rootGuid]
	}

	companion object
	{
		fun load(path: String): MapGenerator
		{
			val xml = getXml("Maps/$path")

			val generator = MapGenerator()
			generator.parse(xml)

			return generator
		}
	}
}