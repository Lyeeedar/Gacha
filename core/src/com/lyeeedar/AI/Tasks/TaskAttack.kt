package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.*
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Tile
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Systems.EventData
import com.lyeeedar.Systems.EventSystem
import com.lyeeedar.Systems.event
import com.lyeeedar.Util.*

class TaskAttack() : AbstractTask()
{
	lateinit var tile: Tile
	lateinit var attackDefinition: AttackDefinition

	fun set(tile: Tile, attackDefinition: AttackDefinition): TaskAttack
	{
		this.tile = tile
		this.attackDefinition = attackDefinition

		return this
	}

	override fun execute(e: Entity)
	{
		val fumble = e.stats().getStat(Statistic.FUMBLE)
		if (fumble > 0f && Random.random() < fumble)
		{
			if (!Global.resolveInstant)
			{
				val stunParticle = AssetManager.loadParticleEffect("Stunned").getParticleEffect()
				stunParticle.addToEngine(e.pos().tile!!, Vector2(0f, 0.8f))

				e.stats().messagesToShow.add(MessageData.obtain().set("Fumbled!", Colour.YELLOW, 0.4f))
			}

			return
		}

		e.pos().facing = Direction.getCardinalDirection(tile, e.tile()!!)

		if (!Global.resolveInstant)
		{
			e.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, Direction.Companion.getDirection(e.tile()!!, tile))
		}

		val entitiesToHit = ObjectSet<Entity>()
		for (slot in SpaceSlot.EntityValues)
		{
			val entity = tile.contents[slot] ?: continue
			if (e.isAllies(entity)) continue

			entitiesToHit.add(entity)
		}

		val diff = tile.getPosDiff(e.tile()!!)

		var delay = 0f

		if (!Global.resolveInstant)
		{
			if (attackDefinition.range > 1)
			{
				val animDuration = 0.15f + tile.euclideanDist(e.tile()!!) * 0.015f

				val effect = attackDefinition.flightEffect!!.getParticleEffect()
				effect.rotation = getRotation(e.tile()!!, tile)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(animDuration, diff)

				effect.addToEngine(tile)

				delay += animDuration
			}

			if (attackDefinition.hitEffect != null)
			{
				val effect = attackDefinition.hitEffect!!.getParticleEffect()
				effect.renderDelay = delay
				effect.rotation = getRotation(e.tile()!!, tile)
				effect.addToEngine(tile)

				delay += effect.blockinglifetime * 0.7f
			}
		}

		Future.call(
			{
				for (entity in entitiesToHit)
				{
					val attackerStats = e.stats()!!
					val defenderStats = entity.stats()!!
					var dam = attackerStats.getAttackDam(attackerStats.attackDefinition.damage)

					if (defenderStats.checkAegis())
					{
						if (EventSystem.isEventRegistered(EventType.BLOCK, entity))
						{
							val eventData = EventData.obtain().set(EventType.BLOCK, entity, e, mapOf(Pair("damage", dam.first)))
							Global.engine.event().addEvent(eventData)
						}

						dam = Pair(0f, dam.second)
						defenderStats.blockedDamage = true

						defenderStats.messagesToShow.add(MessageData.obtain().set("Blocked!", Colour.CYAN, 0.4f))
					}

					val finalDam = defenderStats.dealDamage(dam.first, dam.second)

					attackerStats.attackDamageDealt += finalDam
					if (attackerStats.summoner != null)
					{
						attackerStats.summoner!!.stats().attackDamageDealt += finalDam
					}

					if (Random.random() < 0.5f)
					{
						BloodSplatter.splatter(e.tile()!!, entity.tile()!!, 1f)
					}
					defenderStats.lastHitSource = e.tile()!!

					val lifeSteal = attackerStats.getStat(Statistic.LIFESTEAL)
					val stolenLife = finalDam * lifeSteal
					if (stolenLife > 0f)
					{
						attackerStats.heal(stolenLife)
						attackerStats.healing += stolenLife

						if (EventSystem.isEventRegistered(EventType.HEALED, e))
						{
							val healEventData = EventData.obtain().set(EventType.HEALED, e, entity, mapOf(Pair("amount", stolenLife)))
							Global.engine.event().addEvent(healEventData)
						}
					}
					else if (stolenLife < 0f)
					{
						attackerStats.dealDamage(stolenLife, false)
					}

					// do damage events

					// crit
					if (dam.second)
					{
						if (EventSystem.isEventRegistered(EventType.CRIT, e))
						{
							val dealEventData = EventData.obtain().set(EventType.CRIT, e, entity, mapOf(
								Pair("damage", finalDam),
								Pair("dist", e.pos().position.dist(entity.pos().position).toFloat())))
							Global.engine.event().addEvent(dealEventData)
						}
					}

					// deal damage
					if (EventSystem.isEventRegistered(EventType.DEALDAMAGE, e))
					{
						val dealEventData = EventData.obtain().set(EventType.DEALDAMAGE, e, entity, mapOf(
							Pair("damage", finalDam),
							Pair("dist", e.pos().position.dist(entity.pos().position).toFloat())))
						Global.engine.event().addEvent(dealEventData)
					}

					// take damage
					if (EventSystem.isEventRegistered(EventType.TAKEDAMAGE, entity))
					{
						val takeEventData = EventData.obtain().set(EventType.TAKEDAMAGE, entity, e, mapOf(
							Pair("damage", finalDam),
							Pair("dist", e.pos().position.dist(entity.pos().position).toFloat())))
						Global.engine.event().addEvent(takeEventData)
					}
				}
			}, delay)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskAttack> = object : Pool<TaskAttack>() {
			override fun newObject(): TaskAttack
			{
				return TaskAttack()
			}
		}

		@JvmStatic fun obtain(): TaskAttack
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}