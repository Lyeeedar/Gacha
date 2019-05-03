package com.lyeeedar.Game.ActionSequence

import com.badlogic.gdx.utils.Pool
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Animation.SpinAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Statistic
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.randomOrNull

enum class MovementType
{
	MOVE,
	LEAP,
	ROLL,
	TELEPORT
}

class MoveSourceAction() : AbstractActionSequenceAction()
{
	lateinit var type: MovementType

	override fun enter(): Boolean
	{
		val srcTile = sequence.source.tile()!!
		val dst = sequence.targets.randomOrNull() ?: return false
		val dstTile = sequence.level.getTile(dst) ?: return false

		doMove(srcTile, dstTile, type, false)

		sequence.targets.clear()
		sequence.targets.add(dst)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = MoveSourceAction.obtain()
		action.type = type

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<MoveSourceAction> = object : Pool<MoveSourceAction>() {
			override fun newObject(): MoveSourceAction
			{
				return MoveSourceAction()
			}

		}

		@JvmStatic fun obtain(): MoveSourceAction
		{
			val obj = MoveSourceAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { MoveSourceAction.pool.free(this); obtained = false } }
}

class PullAction() : AbstractActionSequenceAction()
{
	lateinit var type: MovementType
	var origin: String? = null

	override fun enter(): Boolean
	{
		val dstTile: Tile
		if (origin == null)
		{
			dstTile = sequence.source.tile()!!
		}
		else
		{
			val targets = sequence.storedTargets[origin!!]
			val point = targets[0]
			dstTile = sequence.level.getTileClamped(point)
		}

		for (src in sequence.targets)
		{
			val srcTile = sequence.level.getTile(src) ?: continue

			val entity = srcTile.firstEntity()
			if (entity == null || entity.isAllies(sequence.source)) continue

			doMove(srcTile, dstTile, type, true)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = PullAction.obtain()
		action.type = type
		action.origin = origin

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
		origin = xmlData.get("Origin", null)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<PullAction> = object : Pool<PullAction>() {
			override fun newObject(): PullAction
			{
				return PullAction()
			}

		}

		@JvmStatic fun obtain(): PullAction
		{
			val obj = PullAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { PullAction.pool.free(this); obtained = false } }
}

class KnockbackAction() : AbstractActionSequenceAction()
{
	lateinit var type: MovementType
	var dist: Int = 1
	var origin: String? = null

	override fun enter(): Boolean
	{
		val srcTile: Tile
		if (origin == null)
		{
			srcTile = sequence.source.tile()!!
		}
		else
		{
			val targets = sequence.storedTargets[origin!!]
			val point = targets[0]
			srcTile = sequence.level.getTileClamped(point)
		}

		for (target in sequence.targets)
		{
			val targetTile = sequence.level.getTile(target) ?: continue

			val entity = targetTile.firstEntity()
			if (entity == null || entity.isAllies(sequence.source)) continue

			val dir = Direction.getDirection(srcTile, targetTile)
			val dstPoint = Point.obtain().set(dir.x, dir.y)
			dstPoint.timesAssign(dist)
			dstPoint.plusAssign(targetTile)
			val dst = sequence.level.getTileClamped(dstPoint)

			doMove(targetTile, dst, type, true)

			dstPoint.free()
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = KnockbackAction.obtain()
		action.type = type
		action.dist = dist
		action.origin = origin

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
		dist = xmlData.getInt("Dist", 1)
		origin = xmlData.get("Origin", null)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<KnockbackAction> = object : Pool<KnockbackAction>() {
			override fun newObject(): KnockbackAction
			{
				return KnockbackAction()
			}

		}

		@JvmStatic fun obtain(): KnockbackAction
		{
			val obj = KnockbackAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { KnockbackAction.pool.free(this); obtained = false } }
}

private fun doMove(src: Tile, dst: Tile, type: MovementType, interrupt: Boolean): Tile
{
	val entity = src.contents[SpaceSlot.ENTITY] ?: return dst
	val pos = entity.pos() ?: return dst
	if (!pos.moveable) return dst
	val stats = entity.stats() ?: return dst
	if (stats.invulnerable || stats.blocking || Random.random.nextFloat() < stats.getStat(Statistic.AEGIS))
	{
		stats.blockedDamage = true
		return dst
	}

	if (interrupt)
	{
		val task = entity.task()
		if (task != null)
		{
			if (task.tasks.size == 0 || task.tasks[0] !is TaskInterrupt)
			{
				task.tasks.clear()
				task.tasks.add(TaskInterrupt())
			}
		}
	}

	val path = BresenhamLine.lineNoDiag(src.x, src.y, dst.x, dst.y, src.level.grid)

	var dst = dst

	if (type == MovementType.ROLL || type == MovementType.MOVE)
	{
		for (point in path)
		{
			val tile = src.level.getTile(point) ?: break

			if (!pos.isValidTile(tile, entity)) break

			dst = tile
		}
	}
	else
	{
		if (!pos.isValidTile(dst, entity))
		{
			val validTiles = com.badlogic.gdx.utils.Array<Tile>(4)
			for (dir in Direction.Values)
			{
				val tile = src.level.getTile(dst + dir) ?: continue
				if (pos.isValidTile(tile, entity))
				{
					validTiles.add(tile)
				}
			}

			if (validTiles.size > 0)
			{
				dst = validTiles.random()
			}
			else
			{
				for (point in path)
				{
					val tile = src.level.getTile(point) ?: break

					if (!pos.isValidTile(tile, entity)) break

					dst = tile
				}
			}
		}
	}

	if (dst == src) return dst

	pos.doMove(dst, entity)

	if (!Global.resolveInstant)
	{
		val animSpeed = 0.1f + src.euclideanDist(dst) * 0.015f

		if (type == MovementType.MOVE)
		{
			entity.renderable().renderable.animation = MoveAnimation.obtain().set(dst, src, animSpeed)
		}
		else if (type == MovementType.ROLL)
		{
			entity.renderable().renderable.animation = MoveAnimation.obtain().set(dst, src, animSpeed)

			val direction = Direction.getDirection(src, dst)
			if (direction == Direction.EAST)
			{
				entity.renderable().renderable.animation = SpinAnimation.obtain().set(animSpeed, -360f)
			}
			else
			{
				entity.renderable().renderable.animation = SpinAnimation.obtain().set(animSpeed, 360f)
			}

			entity.renderable().renderable.animation = ExpandAnimation.obtain().set(animSpeed * 0.9f, 1f, 0.8f, false)
		}
		else if (type == MovementType.LEAP)
		{
			entity.renderable().renderable.animation = LeapAnimation.obtain().setRelative(animSpeed, src, dst, 3f)

			if (entity.renderable().renderable is Sprite)
			{
				(entity.renderable().renderable as Sprite).removeAmount = 0f
			}
		}
	}

	return dst
}