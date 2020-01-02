package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.AI.Tasks.TaskMove
import com.lyeeedar.Components.isOnTile
import com.lyeeedar.Components.posOrNull
import com.lyeeedar.Components.taskOrNull
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Pathfinding.Pathfinder
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionMoveTo(): AbstractAction()
{
	var dst: Int = 0
	var towards: Boolean = true
	lateinit var key: String

	var lastPos: Point = Point.ZERO

	override fun evaluate(entity: Entity): ExecutionState
	{
		val target = getData<Point>( key, null )
		val posData = entity.posOrNull()
		val taskData = entity.taskOrNull()
		val tile = entity.tile()

		// doesnt have all the needed data, fail
		if ( target == null || posData == null || tile == null || taskData == null )
		{
			state = ExecutionState.FAILED
			return state
		}

		// if we arrived at our target, succeed
		if ( (towards && (tile.taxiDist(target) <= dst || posData.isOnTile(target))) || (!towards && tile.taxiDist(target) >= dst) )
		{
			lastPos = Point.ZERO
			state = ExecutionState.COMPLETED
			return state
		}

		val pathFinder = Pathfinder(tile.level.grid, tile.x, tile.y, target.x, target.y, posData.size, entity)
		val path = pathFinder.getPath( posData.slot )

		if (path == null)
		{
			lastPos = Point.ZERO
			state = ExecutionState.FAILED
			return state
		}

		// if couldnt find a valid path, fail
		if ( path.size < 2 )
		{
			lastPos = Point.ZERO
			Point.freeAll(path)
			state = ExecutionState.FAILED
			return state
		}

		val nextTile = tile.level.getTile( path.get( 1 ) )

		// possible loop, quit just in case
		if (nextTile == lastPos)
		{
			lastPos = Point.ZERO
			Point.freeAll(path)
			state = ExecutionState.FAILED
			return state
		}

		// if next step is impassable then fail
		if (nextTile?.getPassable(posData.slot, entity) != true)
		{
			lastPos = Point.ZERO
			Point.freeAll(path)
			state = ExecutionState.FAILED
			return state
		}

		val offset = path.get( 1 ) - path.get( 0 )

		// if moving towards path to the object
		if ( towards )
		{
			if ( path.size - 1 <= dst || offset == Point.ZERO )
			{
				lastPos = Point.ZERO
				Point.freeAll(path)
				offset.free()
				state = ExecutionState.COMPLETED
				return state
			}

			lastPos = tile
			taskData.tasks.add(TaskMove.obtain().set(Direction.getDirection(offset)))
		}
		// if moving away then just run directly away
		else
		{
			if ( path.size - 1 >= dst || offset == Point.ZERO )
			{
				lastPos = Point.ZERO
				Point.freeAll(path)
				offset.free()
				state = ExecutionState.COMPLETED
				return state
			}

			lastPos = tile
			val opposite = offset * -1
			taskData.tasks.add(TaskMove.obtain().set(Direction.getDirection(opposite)))
			opposite.free()
		}

		Point.freeAll(path)
		offset.free()
		state = ExecutionState.RUNNING
		return state
	}

	override fun cancel(entity: Entity)
	{
		lastPos = Point.ZERO
	}

	override fun parse( xml: XmlData)
	{
		dst = xml.getIntAttribute("Distance", 0)
		towards = xml.getBooleanAttribute("Towards", true)
		key = xml.getAttribute( "Key" ).toLowerCase()
	}
}