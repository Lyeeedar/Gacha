package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.MarkedForDeletionComponent
import com.lyeeedar.Util.XmlData

class ActionKill : AbstractAction()
{
	override fun evaluate(entity: Entity): ExecutionState
	{
		if (entity.getComponent(MarkedForDeletionComponent::class.java) == null) entity.add(MarkedForDeletionComponent())
		return ExecutionState.COMPLETED
	}

	override fun parse(xml: XmlData)
	{
	}

	override fun cancel(entity: Entity)
	{
	}
}