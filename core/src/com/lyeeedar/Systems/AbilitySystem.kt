package com.lyeeedar.Systems

import com.badlogic.ashley.core.Family
import com.lyeeedar.Components.AbilityComponent
import com.lyeeedar.Components.StatisticsComponent
import com.lyeeedar.Components.ability
import com.lyeeedar.Components.stats
import com.lyeeedar.Statistic

class AbilitySystem : AbstractSystem(Family.all(StatisticsComponent::class.java, AbilityComponent::class.java).get())
{
	override fun doUpdate(deltaTime: Float)
	{

	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val ability = entity.ability()!!
			val stats = entity.stats()!!

			for (ab in ability.abilities)
			{
				ab.remainingCooldown -= (1f + stats.getStat(Statistic.ABILITYCOOLDOWN))
			}
		}
	}
}
