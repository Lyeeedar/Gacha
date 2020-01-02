package com.lyeeedar.Systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.Tasks.TaskAttack
import com.lyeeedar.AI.Tasks.TaskMove
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Statistic
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.max
import ktx.collections.set

/**
 * Created by Philip on 20-Mar-16.
 */

class TaskProcessorSystem(): AbstractSystem(Family.all(TaskComponent::class.java).get())
{
	val minTurnTime = 0.3f

	lateinit var renderables: ImmutableArray<Entity>
	lateinit var abilities: ImmutableArray<Entity>

	val onTurnEvent = Event0Arg()

	var lastState = "---"
	var printTasks = false

	val processArray = Array<Entity>(false, 16)

	var turnTime = 0f

	init
	{

	}

	override fun registerDebugCommands(debugConsole: DebugConsole)
	{
		debugConsole.register("TaskSystemLastState", "", fun (args, console): Boolean {
			console.write(lastState)

			return true
		})

		debugConsole.register("PrintTasks", "", fun (args, console): Boolean {
			if (args[0] == "true" || args[0] == "false") printTasks = args[0] == "true"
			else
			{
				console.error("must be in form 'printtasks true/false'")
				return false
			}

			return true
		})
	}

	override fun unregisterDebugCommands(debugConsole: DebugConsole)
	{
		debugConsole.unregister("TaskSystemLastState")
		debugConsole.unregister("PrintTasks")
	}

	override fun addedToEngine(engine: Engine?)
	{
		if (engine == null) throw RuntimeException("Engine is null!")

		entities = engine.getEntitiesFor(Family.all(TaskComponent::class.java).get())
		renderables = engine.getEntitiesFor(Family.all(RenderableComponent::class.java).get())
		abilities = engine.getEntitiesFor(Family.all(ActiveActionSequenceComponent::class.java).get())
	}

	val survivingFactionMap = ObjectMap<String, Int>()
	override fun doUpdate(deltaTime: Float)
	{
		if (level == null || level!!.selectingEntities) return

		turnTime += deltaTime

		val hasEffects = renderables.any {(!it.isTransient() && it.renderable().renderable.isBlocking)}
		val hasAnimations = renderables.any {(it.renderable().renderable.animationBlocksUpdate && it.renderable().renderable.animation != null) }
		val hasActionSequences = abilities.any { !(it.activeActionSequence()?.sequence?.blocked ?: true) }

		if ((!hasEffects || turnTime >= minTurnTime) && !hasAnimations && !hasActionSequences)
		{
			if (processArray.size == 0 && (Global.resolveInstant || turnTime >= minTurnTime))
			{
				beginTurn()
			}
			else if (processArray.size > 0)
			{
				updateEntities()
			}
		}
		else if (hasAnimations)
		{
			lastState = "Waiting on animations"
		}
		else if (hasActionSequences)
		{
			lastState = "Waiting on action sequences"
		}
		else if (hasEffects)
		{
			lastState = "Waiting on effects"

			if (turnTime > 2f)
			{
				// kill long running effects
				for (animEntity in renderables)
				{
					val renderableComp = animEntity.renderable() ?: continue
					val renderable = renderableComp.renderable

					if (renderable.isBlocking)
					{
						if (renderable is ParticleEffect)
						{
							renderable.stop()
						}
					}
				}
			}
		}

		survivingFactionMap.clear()
		for (entity in entities)
		{
			val stats = entity.stats() ?: continue
			survivingFactionMap[stats.faction] = (survivingFactionMap[stats.faction] ?: 0) + 1
		}

		for (entity in entities)
		{
			val stats = entity.stats() ?: continue
			stats.survivingAllies = survivingFactionMap[stats.faction] - 1
		}
	}

	private fun beginTurn()
	{
		processArray.clear()
		for (entity in entities)
		{
			val task = entity.task()
			task.actionAccumulator += 1f

			if (task.actionAccumulator > 0f)
			{
				processArray.add(entity)
			}
		}

		turnTime = 0f

		onTurnEvent()

		level!!.turnCount++

		for (system in engine.systems)
		{
			if (system is AbstractSystem)
			{
				system.onTurn()
			}
		}
	}

	private fun updateEntities()
	{
		val itr = processArray.iterator()
		while (itr.hasNext())
		{
			val entity = itr.next()
			if (entity.isMarkedForDeletion())
			{
				itr.remove()
				continue
			}

			val task = entity.task() ?: continue

			processEntity(entity)

			if (task.actionAccumulator <= 0f)
			{
				itr.remove()
			}
		}
	}

	private fun processEntity(e: Entity): Boolean
	{
		val task = e.taskOrNull() ?: return false

		if (e.statsOrNull()?.hp ?: 1f <= 0) return false

		if (e.renderableOrNull()?.renderable?.animation != null && !e.renderable().renderable.animation!!.isBlocking) return false

		val trailing = e.trailingEntity()
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
			e.event()?.onTurn?.invoke()

			val t = task.tasks.removeIndex(0)

			if (printTasks)
			{
				println("Entity '" + e.name()?.name + "' doing task '" + t.javaClass.simpleName + "'")
			}

			t.execute(e)

			e.pos().turnsOnTile++
			e.pos().moveLocked = false

			if (t is TaskMove)
			{
				e.trailingEntity()?.updatePos(e.tile()!!)
			}

			t.free()

			var haste = e.statsOrNull()?.getStat(Statistic.HASTE) ?: 0f

			if (t is TaskMove)
			{
				val fleet = e.statsOrNull()?.getStat(Statistic.FLEETFOOT) ?: 0f
				haste += fleet
			}
			else if (t is TaskAttack)
			{
				val dervish = e.statsOrNull()?.getStat(Statistic.DERVISH) ?: 0f
				haste += dervish
			}

			task.actionAccumulator -= (1f / max(task.speed + haste, 0.2f))

			return true
		}
		else
		{
			val trailing = e.trailingEntity()
			if (trailing != null && trailing.collapses)
			{
				trailing.updatePos(e.tile()!!)
			}
		}

		task.actionAccumulator = 0f

		return false
	}
}
