package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Global
import com.lyeeedar.Systems.level

class TaskAbility(val target: Entity, val ability: ActionSequence) : AbstractTask()
{
	override fun execute(e: Entity)
	{
		ability.begin(e, Global.engine.level!!)
		ability.targets.clear()
		ability.targets.add(target.tile()!!)
		ability.lockedTargets.add(target)
		ability.facing = Direction.Companion.getDirection(e.tile()!!, target.tile()!!)

		val component = ActiveActionSequenceComponent()
		component.sequence = ability

		e.add(component)
	}
}