package com.lyeeedar.Game.Ability

import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Animation.SpinAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

enum class MovementType
{
	MOVE,
	LEAP,
	ROLL,
	TELEPORT
}

class MoveSourceAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var type: MovementType

	override fun enter(): Boolean
	{
		val srcTile = ability.source.tile()!!
		val dst = ability.targets.random()!!
		val dstTile = ability.level.getTile(dst) ?: return false

		doMove(srcTile, dstTile, type)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = MoveSourceAction(ability)
		action.type = type

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MovementType", "Move")!!.toUpperCase())
	}
}

class PullAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var type: MovementType

	override fun enter(): Boolean
	{
		val dstTile = ability.source.tile()!!
		for (src in ability.targets)
		{
			val srcTile = ability.level.getTile(src) ?: continue

			doMove(srcTile, dstTile, type)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = PullAction(ability)
		action.type = type

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MovementType", "Move")!!.toUpperCase())
	}
}

class KnockbackAction(ability: Ability) : AbstractAbilityAction(ability)
{
	lateinit var type: MovementType
	var dist: Int = 1

	override fun enter(): Boolean
	{
		val srcTile = ability.source.tile()!!
		for (target in ability.targets)
		{
			val targetTile = ability.level.getTile(target) ?: continue

			val dir = Direction.getDirection(srcTile, targetTile)
			val dstPoint = targetTile + Point(dir.x, dir.y) * dist
			val dst = ability.level.getTileClamped(dstPoint)

			doMove(targetTile, dst, type)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = KnockbackAction(ability)
		action.type = type
		action.dist = dist

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		type = MovementType.valueOf(xmlData.get("MovementType", "Move")!!.toUpperCase())
		dist = xmlData.getInt("Dist", 1)
	}
}

private fun doMove(src: Tile, dst: Tile, type: MovementType)
{
	val entity = src.contents[SpaceSlot.ENTITY] ?: return
	val pos = entity.pos() ?: return
	if (!pos.moveable) return
	val stats = entity.stats() ?: return
	if (stats.invulnerable || stats.blocking)
	{
		stats.blockedDamage = true
		return
	}

	val path = BresenhamLine.lineNoDiag(src.x, src.y, dst.x, dst.y, src.level.grid)

	var dst = src
	for (point in path)
	{
		val tile = src.level.getTile(point) ?: break

		if (!pos.isValidTile(tile, entity)) break

		dst = tile
	}

	if (dst == src) return

	pos.doMove(dst, entity)

	entity.task()?.tasks?.clear()
	entity.task()?.tasks?.add(TaskInterrupt())

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

		entity.renderable().renderable.animation = ExpandAnimation.obtain().set(animSpeed*0.9f, 1f, 0.8f, false)
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