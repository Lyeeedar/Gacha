package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionClearValue(): AbstractAction()
{
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		setData(key, null)

		state = ExecutionState.COMPLETED
		return state
	}

	override fun cancel(entity: Entity)
	{

	}

	override fun parse(xml: XmlData)
	{
		key = xml.getAttribute("Key").toLowerCase()
	}
}