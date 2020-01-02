package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool

class TaskUseAbility : AbstractTask()
{
	override fun execute(e: Entity)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskUseAbility> = object : Pool<TaskUseAbility>() {
			override fun newObject(): TaskUseAbility
			{
				return TaskUseAbility()
			}
		}

		@JvmStatic fun obtain(): TaskUseAbility
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}