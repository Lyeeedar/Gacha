package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveAbilityComponent
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Systems.level

class TaskAbility(val tile: Tile, val ability: Ability) : AbstractTask()
{
	override fun execute(e: Entity)
	{
		ability.begin(e, Global.engine.level!!)
		ability.targets.clear()
		ability.targets.add(tile)
		ability.facing = Direction.Companion.getDirection(e.tile()!!, tile)

		val component = ActiveAbilityComponent()
		component.ability = ability

		e.add(component)
	}
}