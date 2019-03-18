package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.stats
import com.lyeeedar.Components.tile
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import java.util.*

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionPick(): AbstractAction()
{
	lateinit var input: String
	lateinit var output: String
	lateinit var criteria: String
	var lowest: Boolean = true

	val ran: Random = Random()

	override fun evaluate(entity: Entity): ExecutionState
	{
		state = ExecutionState.FAILED
		val tile = entity.tile() ?: return ExecutionState.FAILED

		val obj = getData<Any>(input, null)

		if (obj == null || obj !is Iterable<*>)
		{
			state = ExecutionState.FAILED
		}
		else
		{
			if (obj.count() == 0)
			{
				state = ExecutionState.FAILED
			}
			else
			{
				if (criteria == "random")
				{
					val index = ran.nextInt(obj.count())
					setData(output, obj.elementAt(index))
					state = ExecutionState.COMPLETED
				}
				else if (criteria == "distance")
				{
					val sorted = obj.sortedBy { (it as? Point)?.euclideanDist2(tile) ?: (it as? Entity)?.tile()?.euclideanDist2(tile) }

					val item = if (lowest) sorted.first() else sorted.last()
					setData(output, item)
					state = ExecutionState.COMPLETED
				}
				else
				{
					val sorted = obj.sortedBy { (it as? Entity)?.stats()?.get(criteria, 0f) }

					val item = if (lowest) sorted.first() else sorted.last()
					setData(output, item)
					state = ExecutionState.COMPLETED
				}
			}
		}

		return state
	}

	override fun parse(xml: XmlData)
	{
		input = xml.getAttribute("Input").toLowerCase()
		output = xml.getAttribute("Output").toLowerCase()
		criteria = xml.getAttribute("Criteria", "Distance")!!.toLowerCase()
		lowest = xml.getBooleanAttribute("Lowest", true)
	}

	override fun cancel(entity: Entity) {

	}

}