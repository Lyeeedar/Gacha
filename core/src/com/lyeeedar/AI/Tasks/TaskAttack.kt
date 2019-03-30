package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import ktx.math.times

class TaskAttack(val tile: Tile, val attackDefinition: AttackDefinition) : AbstractTask()
{
	val splatters = AssetManager.loadSprite("Oryx/Custom/terrain/bloodsplatter")

	override fun execute(e: Entity)
	{
		e.pos().facing = Direction.getCardinalDirection(e.pos().position.x - tile.x, tile.y - e.pos().position.y)

		e.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, Direction.Companion.getDirection(e.tile()!!, tile))

		val entitiesToHit = ObjectSet<Entity>()
		for (slot in SpaceSlot.EntityValues)
		{
			val entity = tile.contents[slot] ?: continue
			if (e.isAllies(entity)) continue

			entitiesToHit.add(entity)
		}

		val diff = tile.getPosDiff(e.tile()!!)

		var delay = 0f
		if (attackDefinition.range > 1)
		{
			val animDuration = 0.15f + tile.euclideanDist(e.tile()!!) * 0.015f

			val effect = attackDefinition.flightEffect!!.copy()
			effect.rotation = getRotation(e.tile()!!, tile)
			effect.killOnAnimComplete = true
			effect.animation = MoveAnimation.obtain().set(animDuration, diff)

			effect.addToEngine(tile)

			delay += animDuration
		}

		if (attackDefinition.hitEffect != null)
		{
			val effect = attackDefinition.hitEffect!!.copy()
			effect.renderDelay = delay
			effect.rotation = getRotation(e.tile()!!, tile)
			effect.addToEngine(tile)

			delay += effect.lifetime * 0.3f
		}

		Future.call(
			{
				for (entity in entitiesToHit)
				{
					val attackerStats = e.stats()!!
					val defenderStats = entity.stats()!!
					val dam = attackerStats.getAttackDam(attackerStats.attackDefinition.damage)
					val finalDam = defenderStats.dealDamage(dam)

					if (Random.random() < 0.5f)
					{
						val sourceTile = e.tile()!!
						val entityTile = entity.tile()!!

						var vector = (entityTile.copy()-sourceTile).toVec().nor()
						vector = vector.rotate(Random.random(-45f, 45f))
						vector = vector.scl(Random.random())

						val chosen = splatters.textures.random()

						val sprite = Sprite(chosen)
						sprite.baseScale[0] = 0.5f
						sprite.baseScale[1] = 0.5f
						sprite.colour = Colour(1f, 1f, 1f, 0.6f)

						val renderable = RenderableComponent(sprite)
						val pos = PositionComponent()
						pos.position = entityTile
						pos.offset.set(vector)

						sprite.animation = LeapAnimation.obtain().set(0.05f, arrayOf(vector * -1f, Vector2()), 0.2f)

						val newEntity = Entity()
						newEntity.add(renderable)
						newEntity.add(pos)
						Global.engine.addEntity(newEntity)
					}

					val lifeSteal = attackerStats.getStat(Statistic.LIFESTEAL)
					val stolenLife = finalDam * lifeSteal
					if (stolenLife > 0f)
					{
						attackerStats.heal(stolenLife)
					}
				}
			}, delay)
	}
}