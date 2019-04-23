package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.AbilityData
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Systems.level

class TaskAbility() : AbstractTask()
{
	lateinit var target: Entity
	lateinit var ability: AbilityData

	fun set(target: Entity, ability: AbilityData): TaskAbility
	{
		this.target = target
		this.ability = ability

		return this
	}

	override fun execute(e: Entity)
	{
		e.pos().facing = Direction.getCardinalDirection(target.tile()!!, e.tile()!!)

		if (ability.singleUse)
		{
			ability.remainingCooldown = Float.MAX_VALUE
		}
		else
		{
			ability.remainingCooldown = ability.cooldown.getValue().toFloat()
		}
		ability.selectedCooldown = ability.remainingCooldown
		ability.justUsed = true

		val newAb = ability.ability.copy()
		newAb.cancellable = ability.cancellable
		newAb.source = e

		newAb.begin(e, Global.engine.level!!)
		newAb.targets.clear()
		newAb.targets.add(target.tile()!!)
		newAb.lockedTargets.add(target)
		newAb.facing = Direction.Companion.getDirection(e.tile()!!, target.tile()!!)

		val component = ActiveActionSequenceComponent()
		component.sequence = newAb

		e.add(component)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskAbility> = object : Pool<TaskAbility>() {
			override fun newObject(): TaskAbility
			{
				return TaskAbility()
			}
		}

		@JvmStatic fun obtain(): TaskAbility
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}