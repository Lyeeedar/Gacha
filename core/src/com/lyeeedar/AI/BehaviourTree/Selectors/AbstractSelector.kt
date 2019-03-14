package com.lyeeedar.AI.BehaviourTree.Selectors

import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.AbstractTreeNode
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

abstract class AbstractSelector(): AbstractNodeContainer()
{
	val nodes: com.badlogic.gdx.utils.Array<AbstractTreeNode> = com.badlogic.gdx.utils.Array<AbstractTreeNode>()

	// ----------------------------------------------------------------------
	fun addNode( node: AbstractTreeNode )
	{
		if ( node.data == null )
		{
			node.data = data
		}
		node.parent = this
		nodes.add( node )
	}

	// ----------------------------------------------------------------------
	override fun parse( xml: XmlData)
	{
		for ( i in 0 until xml.childCount)
		{
			val value = xml.getChild(i)

			val node = AbstractTreeNode.get(value.getAttribute("meta:RefKey").toUpperCase())

			addNode( node )

			node.parse( value )
		}
	}
}
