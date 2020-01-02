package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.AI.Tasks.TaskAttack
import com.lyeeedar.Components.posOrNull
import com.lyeeedar.Components.statsOrNull
import com.lyeeedar.Components.taskOrNull
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

class ActionAttack : AbstractAction()
{
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val target = getData<Point>(key, null)
		val posData = entity.posOrNull()
		val taskData = entity.taskOrNull()
		val stats = entity.statsOrNull()

		// doesnt have all the needed data, fail
		if ( target == null || posData == null || taskData == null || stats == null )
		{
			state = ExecutionState.FAILED
			return state
		}

		val tile = posData.position as? Tile
		if ( tile == null )
		{
			state = ExecutionState.FAILED
			return state
		}

		val targetTile = tile.level.getTile(target)
		if (targetTile == null)
		{
			state = ExecutionState.FAILED
			return state
		}

		if (target.taxiDist(tile) > stats.attackDefinition.range)
		{
			state = ExecutionState.FAILED
			return state
		}

		taskData.tasks.add(TaskAttack.obtain().set(targetTile, stats.attackDefinition))

		state = ExecutionState.COMPLETED
		return state
	}

	override fun parse(xml: XmlData)
	{
		key = xml.getAttribute( "Key" ).toLowerCase()
	}

	override fun cancel(entity: Entity)
	{

	}
}