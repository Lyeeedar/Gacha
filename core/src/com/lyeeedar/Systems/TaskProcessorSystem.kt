package com.lyeeedar.Systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.lyeeedar.AI.Tasks.TaskMove
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.Event1Arg

/**
 * Created by Philip on 20-Mar-16.
 */

class TaskProcessorSystem(): AbstractSystem(Family.all(TaskComponent::class.java).get())
{
	lateinit var renderables: ImmutableArray<Entity>

	val onTurnEvent = Event0Arg()
	val signalEvent = Event1Arg<String>()

	var lastState = "---"
	var printTasks = false

	init
	{
		DebugConsole.current()?.register("TaskSystemLastState", "", fun (args, console): Boolean {
			console.write(lastState)

			return true
		})

		DebugConsole.current()?.register("PrintTasks", "", fun (args, console): Boolean {
			if (args[0] == "true" || args[0] == "false") printTasks = args[0] == "true"
			else
			{
				return false
			}

			return true
		})
	}

	override fun addedToEngine(engine: Engine?)
	{
		entities = engine?.getEntitiesFor(Family.all(TaskComponent::class.java).get()) ?: throw RuntimeException("Engine is null!")
		renderables = engine?.getEntitiesFor(Family.all(RenderableComponent::class.java).get()) ?: throw RuntimeException("Engine is null!")
	}

	override fun doUpdate(deltaTime: Float)
	{
		if (Global.pause) return

		val hasEffects = renderables.any { it.renderable()!!.renderable.animation != null && it.renderable()!!.renderable.animation!!.isBlocking }

		if (!hasEffects)
		{
			doTurn()
		}
		else if (hasEffects)
		{
			lastState = "Waiting on effects"
		}
	}

	fun doTurn()
	{
		for (entity in entities)
		{
			for (i in 1..entity.task().speed)
			{
				processEntity(entity)
			}
		}

		onTurnEvent()

		for (system in systemList)
		{
			(engine.getSystem(system.java) as AbstractSystem).onTurn()
		}
	}

	fun processEntity(e: Entity): Boolean
	{
		val task = e.task() ?: return false

		if (e.stats()?.hp ?: 1f <= 0) return false

		if (e.renderable()?.renderable?.animation != null && !e.renderable().renderable.animation!!.isBlocking) return false

		val trailing = e.trailing()
		if (trailing != null && !trailing.initialised)
		{
			trailing.updatePos(e.tile()!!)
			trailing.initialised = true
		}

		if (task.tasks.size == 0)
		{
			task.ai.update(e)
		}

		if (task.tasks.size > 0)
		{
			e.event().onTurn()

			val t = task.tasks.removeIndex(0)

			if (printTasks)
			{
				println("Entity '" + e.name().name + "' doing task '" + t.javaClass.simpleName + "'")
			}

			t.execute(e)

			e.pos().turnsOnTile++
			e.pos().moveLocked = false

			if (t is TaskMove)
			{
				e.trailing()?.updatePos(e.tile()!!)
			}

			return true
		}
		else
		{
			val trailing = e.trailing()
			if (trailing != null && trailing.collapses)
			{
				trailing.updatePos(e.tile()!!)
			}
		}

		return false
	}
}
