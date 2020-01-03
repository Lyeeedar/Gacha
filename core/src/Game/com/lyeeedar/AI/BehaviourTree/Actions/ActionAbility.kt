package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.AI.Tasks.TaskAbility
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.randomOrNull

class ActionAbility : AbstractAction()
{
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val targetPoint = getData<Point>(key, null)
		val posData = entity.posOrNull()
		val taskData = entity.taskOrNull()
		val ability = entity.ability()
		val stats = entity.statsOrNull()

		// doesnt have all the needed data, fail
		if ( targetPoint == null || posData == null || taskData == null || ability == null || stats == null )
		{
			state = ExecutionState.FAILED
			return state
		}

		val tile = posData.position as? Tile
		if (tile == null)
		{
			state = ExecutionState.FAILED
			return state
		}

		val target = tile.level.getTile(targetPoint)?.firstEntity()
		if (target == null)
		{
			state = ExecutionState.FAILED
			return state
		}

		val dist = target.tile()!!.taxiDist(tile)

		val readyAbility = ability.abilities.filter { it.remainingCooldown <= 0 && it.range >= dist && it.condition.evaluate(stats.variables()) != 0f }.randomOrNull()
		if (readyAbility != null)
		{
			taskData.tasks.add(TaskAbility.obtain().set(target, readyAbility))

			state = ExecutionState.COMPLETED
			return state
		}

		state = ExecutionState.FAILED
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