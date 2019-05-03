package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random

class TaskMove(): AbstractTask()
{
	lateinit var direction: Direction

	fun set(direction: Direction): TaskMove
	{
		this.direction = direction

		return this
	}

	override fun execute(e: Entity)
	{
		val root = e.stats().getStat(Statistic.ROOT)
		if (root > 0f && Random.random() < root)
		{
			if (!Global.resolveInstant)
			{
				val stunParticle = AssetManager.loadParticleEffect("Stunned").getParticleEffect()
				stunParticle.addToEngine(e.pos().tile!!, Vector2(0f, 0.8f))

				e.stats().messagesToShow.add(MessageData.obtain().set("Rooted!", Colour.YELLOW, 0.4f))
			}

			return
		}

		e.directionalSprite()?.currentAnim = "move"

		val pos = e.pos() ?: return
		if (pos.moveLocked) return

		if (pos.position is Tile)
		{
			val prev = (pos.position as Tile)
			val next = prev.level.getTile(prev, direction) ?: return

			if (e.pos().isValidTile(next, e))
			{
				e.pos().doMove(next, e)

				if (!Global.resolveInstant)
				{
					e.renderable().renderable.animation = MoveAnimation.obtain().set(next, prev, 0.15f)
				}
			}
		}
		else
		{
			pos.position.x += direction.x
			pos.position.y += direction.y
		}
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<TaskMove> = object : Pool<TaskMove>() {
			override fun newObject(): TaskMove
			{
				return TaskMove()
			}
		}

		@JvmStatic fun obtain(): TaskMove
		{
			val anim = pool.obtain()

			if (anim.obtained) throw RuntimeException()

			anim.obtained = true
			return anim
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }
}