package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.BehaviourTree
import com.lyeeedar.AI.Tasks.AbstractTask
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.UI.IDebugCommandProvider
import com.lyeeedar.Util.XmlData

class TaskComponent: AbstractComponent(), IDebugCommandProvider
{
	lateinit var ai: BehaviourTree
	val tasks: com.badlogic.gdx.utils.Array<AbstractTask> = com.badlogic.gdx.utils.Array()
	var speed: Float = 1f

	var actionAccumulator = 0f

	override fun parse(xml: XmlData, entity: Entity)
	{
		ai = BehaviourTree.load(xml.get("AI"))
		speed = xml.getFloat("Speed", 1f)
	}

	override fun detachCommands()
	{
		DebugConsole.current()?.unregister("Tasks")
		DebugConsole.current()?.unregister("AIData")
	}

	override fun attachCommands()
	{
		DebugConsole.current()?.register("Tasks", "", fun (args, console): Boolean {

			console.write("Task count: " + tasks.size)
			for (task in tasks)
			{
				console.write(task.toString())
			}

			return true
		})

		DebugConsole.current()?.register("AIData", "", fun (args, console): Boolean {

			console.write("Data count: " + ai.root.data!!.size)
			for (pair in ai.root.data!!.entries())
			{
				console.write(pair.key + ": " + pair.value)
			}

			return true
		})
	}
}