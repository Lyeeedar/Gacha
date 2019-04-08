package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Systems.level

class TaskAbility(val tile: Tile, val ability: ActionSequence) : AbstractTask()
{
	override fun execute(e: Entity)
	{
		ability.begin(e, Global.engine.level!!)
		ability.targets.clear()
		ability.targets.add(tile)
		ability.facing = Direction.Companion.getDirection(e.tile()!!, tile)

		val component = ActiveActionSequenceComponent()
		component.sequence = ability

		e.add(component)
	}
}