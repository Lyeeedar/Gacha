package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Nodes.DeferredNode
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidmath.LightRNG

class MapGenerator
{
	lateinit var ran: LightRNG
	val nodeMap = ObjectMap<String, MapGeneratorNode>()
	lateinit var root: MapGeneratorNode

	val deferredNodes = Array<DeferredNode>()

	fun parse(xmlData: XmlData)
	{
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
}