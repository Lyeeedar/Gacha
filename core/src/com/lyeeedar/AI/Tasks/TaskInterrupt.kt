package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveAbilityComponent
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.task

class TaskInterrupt : AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.task().ai.cancel(e)

		val activeAbility = Mappers.activeAbility.get(e)
		if (activeAbility != null)
		{
			activeAbility.ability.cancel()
			e.remove(ActiveAbilityComponent::class.java)
		}
	}
}