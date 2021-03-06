package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.tile
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 29-Mar-16.
 */

class ActionConvertTo(): AbstractAction()
{
	lateinit var input: String
	lateinit var output: String
	lateinit var type: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val obj: Any? = getData<Any>(input, null)
		var out: Any? = null

		if (obj is Entity)
		{
			out = obj.tile() ?: obj.pos().position
		}

		if (out != null)
		{
			setData(output, out)
			state = ExecutionState.COMPLETED
		}
		else
		{
			state = ExecutionState.FAILED
		}

		return state
	}

	override fun parse(xml: XmlData)
	{
		input = xml.getAttribute("Input").toLowerCase()
		output = xml.getAttribute("Output").toLowerCase()
		type = xml.getAttribute("Type", "Position")!!.toLowerCase()
	}

	override fun cancel(entity: Entity)
	{

	}
}