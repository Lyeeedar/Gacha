package com.lyeeedar.Systems

import com.badlogic.ashley.core.Family
import com.lyeeedar.AI.Tasks.TaskUseAbility
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.task

class ActionSequenceSystem : AbstractSystem(Family.one(ActiveActionSequenceComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		for (entity in entities)
		{
			val activeSequence = Mappers.activeActionSequence.get(entity) ?: continue
			val complete = activeSequence.sequence.update(deltaTime)

			if (complete)
			{
				entity.remove(ActiveActionSequenceComponent::class.java)
				if (Mappers.transient.get(entity) != null)
				{
					engine.removeEntity(entity)
				}
			}
		}
	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val activeSequence = Mappers.activeActionSequence.get(entity) ?: continue
			activeSequence.sequence.onTurn()

			val task = entity.task() ?: continue
			task.tasks.add(TaskUseAbility())
		}
	}
}