package com.lyeeedar.Systems

import com.badlogic.ashley.core.Family
import com.lyeeedar.AI.Tasks.TaskUseAbility
import com.lyeeedar.Components.*

class ActionSequenceSystem : AbstractSystem(Family.one(ActiveActionSequenceComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		for (entity in entities)
		{
			val activeSequence = entity.activeActionSequence() ?: continue
			var complete = activeSequence.sequence.update(deltaTime)

			if (activeSequence.sequence.removeOnDeath && (activeSequence.sequence.source.isMarkedForDeletion() || activeSequence.sequence.source.stats().hp <= 0))
			{
				activeSequence.sequence.cancel()
				complete = true
			}

			if (complete)
			{
				entity.remove(ActiveActionSequenceComponent::class.java)
				if (entity.isTransient() || entity.components.size() == 0)
				{
					engine.removeEntity(entity)
				}

				activeSequence.sequence.free()
			}
		}
	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val activeSequence = entity.activeActionSequence() ?: continue
			activeSequence.sequence.onTurn()

			val task = entity.taskOrNull() ?: continue
			task.tasks.clear()
			task.tasks.add(TaskUseAbility())
		}
	}
}