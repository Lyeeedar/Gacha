package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.AbstractTreeNode
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

abstract class AbstractDecorator(): AbstractNodeContainer()
{
	var node: AbstractTreeNode? = null
		set(value)
		{
			if ( value?.data == null )
			{
				value?.data = data;
			}
			value?.parent = this;
			field = value;
		}

	// ----------------------------------------------------------------------
	override fun <T> findData(key: String): T?
	{
		val thisVar = super.findData<T>(key)
		if (thisVar != null)
		{
			return thisVar
		}

		if (node != null)
		{
			val nodeVar = node!!.findData<T>(key)
			if (nodeVar != null)
			{
				return nodeVar
			}
		}

		return null
	}

	// ----------------------------------------------------------------------
	override fun parse( xml: XmlData)
	{
		val child = xml.getChild( 0 )

		val node = AbstractTreeNode.get(child.getAttribute("meta:RefKey").toUpperCase())
		this.node = node
		node.parent = this

		node.parse(child)
	}

	// ----------------------------------------------------------------------
	override fun cancel(entity: Entity)
	{
		node?.cancel(entity);
	}
}