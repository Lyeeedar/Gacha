package com.lyeeedar.Game.ActionSequence

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

enum class MovementType
{
	MOVE,
	LEAP,
	ROLL,
	TELEPORT
}

class MoveSourceAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var type: MovementType

	override fun enter(): Boolean
	{
		val srcTile = sequence.source.tile()!!
		val dst = sequence.targets.random() ?: return false
		val dstTile = sequence.level.getTile(dst) ?: return false

		doMove(srcTile, dstTile, type, false)

		sequence.targets.clear()
		sequence.targets.add(dst)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = MoveSourceAction(sequence)
		action.type = type

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
	}
}

class PullAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var type: MovementType

	override fun enter(): Boolean
	{
		val dstTile = sequence.source.tile()!!
		for (src in sequence.targets)
		{
			val srcTile = sequence.level.getTile(src) ?: continue

			doMove(srcTile, dstTile, type, true)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = PullAction(sequence)
		action.type = type

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
	}
}

class KnockbackAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var type: MovementType
	var dist: Int = 1

	override fun enter(): Boolean
	{
		val srcTile = sequence.source.tile()!!
		for (target in sequence.targets)
		{
			val targetTile = sequence.level.getTile(target) ?: continue

			val dir = Direction.getDirection(srcTile, targetTile)
			val dstPoint = targetTile + Point(dir.x, dir.y) * dist
			val dst = sequence.level.getTileClamped(dstPoint)

			doMove(targetTile, dst, type, true)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = KnockbackAction(sequence)
		action.type = type
		action.dist = dist

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MoveType", "Move")!!.toUpperCase())
		dist = xmlData.getInt("Dist", 1)
	}
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
			task.tasks.clear()
			task.tasks.add(TaskInterrupt())
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
			val validTiles = com.badlogic.gdx.utils.Array<Tile>()
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
			entity.renderable().renderable.animation = LeapAnimation.obtain().setRelative(animSpeed, src, dst, 2f)

			if (entity.renderable().renderable is Sprite)
			{
				(entity.renderable().renderable as Sprite).removeAmount = 0f
			}
		}
	}

	return dst
}