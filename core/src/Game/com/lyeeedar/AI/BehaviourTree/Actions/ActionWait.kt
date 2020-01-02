package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.exp4j.Helpers.EquationHelper
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.AI.Tasks.TaskWait
import com.lyeeedar.Components.task
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionWait(): AbstractAction()
{
	lateinit var count: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val num = Math.round(EquationHelper.evaluate(count)).toInt()

		val task = entity.task()

		for (i in 0 until num)
		{
			task.tasks.add(TaskWait.obtain())
		}

		state = ExecutionState.COMPLETED
		return state
	}

	override fun parse(xml: XmlData)
	{
		count = xml.getAttribute("Count", "1")!!.toLowerCase()
	}

	override fun cancel(entity: Entity) {

	}

}