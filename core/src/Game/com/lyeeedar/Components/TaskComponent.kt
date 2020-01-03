package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.AI.BehaviourTree.BehaviourTree
import com.lyeeedar.AI.Tasks.AbstractTask
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.UI.IDebugCommandProvider
import com.lyeeedar.Util.XmlData

fun Entity.task(): TaskComponent = taskOrNull()!!
fun Entity.taskOrNull(): TaskComponent? = TaskComponent.mapper.get(this)
class TaskComponent: AbstractComponent(), IDebugCommandProvider
{
	lateinit var ai: BehaviourTree
	val tasks: com.badlogic.gdx.utils.Array<AbstractTask> = com.badlogic.gdx.utils.Array(1)
	var speed: Float = 1f

	var actionAccumulator = 0f

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		ai = BehaviourTree.load(xml.get("AI"))
		speed = xml.getFloat("Speed", 1f)
	}

	override fun detachCommands(debugConsole: DebugConsole)
	{
		debugConsole.unregister("Tasks")
		debugConsole.unregister("AIData")
	}

	override fun attachCommands(debugConsole: DebugConsole)
	{
		debugConsole.register("Tasks", "", fun (args, console): Boolean {

			console.write("Task count: " + tasks.size)
			for (task in tasks)
			{
				console.write(task.toString())
			}

			return true
		})

		debugConsole.register("AIData", "", fun (args, console): Boolean {

			console.write("Data count: " + ai.root.data!!.size)
			for (pair in ai.root.data!!.entries())
			{
				console.write(pair.key + ": " + pair.value)
			}

			return true
		})
	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<TaskComponent> = ComponentMapper.getFor(TaskComponent::class.java)
		fun get(entity: Entity): TaskComponent? = mapper.get(entity)

		private val pool: Pool<TaskComponent> = object : Pool<TaskComponent>() {
			override fun newObject(): TaskComponent
			{
				return TaskComponent()
			}

		}

		@JvmStatic fun obtain(): TaskComponent
		{
			val obj = TaskComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { TaskComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		tasks.clear()
		speed = 1f
		actionAccumulator = 0f
	}
}