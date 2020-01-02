package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.AbstractTreeNode
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

class DecoratorImport(): AbstractDecorator()
{
	override fun evaluate(entity: Entity): ExecutionState
	{
		return node?.evaluate(entity) ?: ExecutionState.FAILED
	}

	override fun cancel(entity: Entity)
	{
		node?.cancel(entity)
	}

	override fun parse(xml: XmlData)
	{
		val path = xml.getAttribute("Path")

		this.node = AbstractTreeNode.load(path)
		this.node?.parent = this
	}
}