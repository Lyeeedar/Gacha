package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.getRotation

class TaskAttack(val tile: Tile, val attackDefinition: AttackDefinition) : AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, Direction.Companion.getDirection(e.tile()!!, tile))

		val entitiesToHit = Array<Entity>()
		for (slot in SpaceSlot.EntityValues)
		{
			val entity = tile.contents[slot] ?: continue
			if (e.isAllies(entity)) continue

			entitiesToHit.add(e)
		}

		val diff = tile.getPosDiff(e.tile()!!)

		var delay = 0.05f
		if (attackDefinition.range > 1)
		{
			val animDuration = 0.2f + tile.euclideanDist(e.tile()!!) * 0.02f

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
			effect.rotation = getRotation(e.tile()!!, tile)
			effect.addToEngine(tile)

			delay += effect.lifetime * 0.3f
		}

		Future.call(
			{
				for (entity in entitiesToHit)
				{
					val stats = entity.stats()!!
					val dam = attackDefinition.damage.getValue()
					stats.dealDamage(dam, true)
				}
			}, delay)
	}
}