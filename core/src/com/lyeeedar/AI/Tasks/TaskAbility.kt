package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Global
import com.lyeeedar.Systems.level

class TaskAbility() : AbstractTask()
{
	lateinit var target: Entity
	lateinit var ability: ActionSequence

	fun set(target: Entity, ability: ActionSequence): TaskAbility
	{
		this.target = target
		this.ability = ability

		return this
	}

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