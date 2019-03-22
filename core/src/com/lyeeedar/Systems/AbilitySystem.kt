package com.lyeeedar.Systems

import com.badlogic.ashley.core.Family
import com.lyeeedar.AI.Tasks.TaskWait
import com.lyeeedar.Components.ActiveAbilityComponent
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.task

class AbilitySystem : AbstractSystem(Family.one(ActiveAbilityComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{
		for (entity in entities)
		{
			val activeAbility = Mappers.activeAbility.get(entity) ?: continue
			val complete = activeAbility.ability.update(deltaTime)

			if (complete)
			{
				entity.remove(ActiveAbilityComponent::class.java)
			}
		}
	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val activeAbility = Mappers.activeAbility.get(entity) ?: continue
			activeAbility.ability.onTurn()

			if (activeAbility.ability.entityLocked)
			{
				val task = entity.task() ?: continue
				task.tasks.add(TaskWait())
			}
		}
	}
}