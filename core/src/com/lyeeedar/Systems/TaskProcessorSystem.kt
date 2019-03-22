package com.lyeeedar.Systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Array
import com.lyeeedar.AI.Tasks.TaskMove
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.Event1Arg
import ktx.collections.addAll

/**
 * Created by Philip on 20-Mar-16.
 */

class TaskProcessorSystem(): AbstractSystem(Family.all(TaskComponent::class.java).get())
{
	lateinit var renderables: ImmutableArray<Entity>
	lateinit var abilities: ImmutableArray<Entity>

	val onTurnEvent = Event0Arg()
	val signalEvent = Event1Arg<String>()

	var lastState = "---"
	var printTasks = false

	val processArray = Array<Entity>(false, 16)

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
		if (engine == null) throw RuntimeException("Engine is null!")

		entities = engine.getEntitiesFor(Family.all(TaskComponent::class.java).get())
		renderables = engine.getEntitiesFor(Family.all(RenderableComponent::class.java).get())
		abilities = engine.getEntitiesFor(Family.all(ActiveAbilityComponent::class.java).get())
	}

	override fun doUpdate(deltaTime: Float)
	{
		if (Global.pause) return

		val hasEffects = renderables.any { it.renderable()!!.renderable.animation != null && it.renderable()!!.renderable.animation!!.isBlocking }
		val hasAbilities = abilities.any { !Mappers.activeAbility.get(it).ability.blocked }

		if (!hasEffects && !hasAbilities)
		{
			doTurn()
		}
		else if (hasAbilities)
		{
			lastState = "Waiting on abilities"
		}
		else if (hasEffects)
		{
			lastState = "Waiting on effects"
		}
	}

	private fun doTurn()
	{
		for (entity in entities)
		{
			val task = entity.task()
			task.actionAccumulator += 1f
		}

		processArray.clear()
		processArray.addAll(entities)

		while (processArray.size > 0)
		{
			val itr = processArray.iterator()
			while (itr.hasNext())
			{
				val entity = itr.next()
				val task = entity.task()

				processEntity(entity)

				task.actionAccumulator -= (1f / task.speed)
				if (task.actionAccumulator <= 0f)
				{
					itr.remove()
				}
			}
		}

		onTurnEvent()

		for (system in systemList)
		{
			(engine.getSystem(system.java) as AbstractSystem).onTurn()
		}
	}

	private fun processEntity(e: Entity): Boolean
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
