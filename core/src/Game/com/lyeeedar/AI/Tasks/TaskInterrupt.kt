package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class TaskInterrupt : AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.task().ai.cancel(e)

		var interrupted = false
		val activeAbility = e.activeActionSequence()
		if (activeAbility != null && activeAbility.sequence.cancellable)
		{
			activeAbility.sequence.cancel()
			e.remove(ActiveActionSequenceComponent::class.java)

			interrupted = true
		}

		if (!Global.resolveInstant)
		{
			val stunParticle = AssetManager.loadParticleEffect("Stunned").getParticleEffect()
			stunParticle.addToEngine(e.pos().tile!!, Vector2(0f, 0.8f))

			val message = if (interrupted) "Interrupted!" else "Stunned!"

			e.stats().messagesToShow.add(MessageData.obtain().set(message, Colour.YELLOW, 0.4f))
		}
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskInterrupt> = object : Pool<TaskInterrupt>() {
			override fun newObject(): TaskInterrupt
			{
				return TaskInterrupt()
			}
		}

		@JvmStatic fun obtain(): TaskInterrupt
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}