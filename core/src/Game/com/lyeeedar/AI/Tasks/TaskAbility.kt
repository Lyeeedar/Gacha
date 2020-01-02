package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.EventType
import com.lyeeedar.Global
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.EventData
import com.lyeeedar.Systems.EventSystem
import com.lyeeedar.Systems.event
import com.lyeeedar.Systems.level
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random

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
		val distract = e.stats().getStat(Statistic.DISTRACTION)
		if (distract > 0f && Random.random() < distract)
		{
			if (!ability.singleUse) // dont consume use of single use ability
			{
				// reset cooldown, but only to half of normal
				ability.remainingCooldown = ability.cooldown.getValue().toFloat() / 2f
				ability.selectedCooldown = ability.remainingCooldown
			}

			if (!Global.resolveInstant)
			{
				val stunParticle = AssetManager.loadParticleEffect("Stunned").getParticleEffect()
				stunParticle.addToEngine(e.pos().tile!!, Vector2(0f, 0.8f))

				e.stats().messagesToShow.add(MessageData.obtain().set("Distracted!", Colour.YELLOW, 0.4f))
			}

			return
		}

		if (EventSystem.isEventRegistered(EventType.USEABILITY, e))
		{
			val eventData = EventData.obtain().set(EventType.USEABILITY, e, e)
			Global.engine.event().addEvent(eventData)
		}

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
		newAb.creatingName = ability.name
		newAb.creatingIcon = ability.icon
		newAb.cancellable = ability.cancellable
		newAb.removeOnDeath = ability.removeOnDeath
		newAb.source = e

		newAb.begin(e, Global.engine.level!!)
		newAb.targets.clear()
		newAb.targets.add(target.tile()!!)
		newAb.lockedTargets.add(target)
		newAb.facing = Direction.Companion.getDirection(e.tile()!!, target.tile()!!)

		val component = ActiveActionSequenceComponent.obtain()
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