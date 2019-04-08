package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager

class TaskInterrupt : AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.task().ai.cancel(e)

		val activeAbility = Mappers.activeActionSequence.get(e)
		if (activeAbility != null)
		{
			activeAbility.sequence.cancel()
			e.remove(ActiveActionSequenceComponent::class.java)
		}

		if (!Global.resolveInstant)
		{
			val stunParticle = AssetManager.loadParticleEffect("Stunned")
			stunParticle.addToEngine(e.pos().tile!!, Vector2(0f, 0.8f))
		}
	}
}