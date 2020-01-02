package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.directionalSprite

/**
 * Created by Philip on 22-Mar-16.
 */

class TaskWait(): AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.directionalSprite()?.currentAnim = "wait"
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskWait> = object : Pool<TaskWait>() {
			override fun newObject(): TaskWait
			{
				return TaskWait()
			}
		}

		@JvmStatic fun obtain(): TaskWait
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}