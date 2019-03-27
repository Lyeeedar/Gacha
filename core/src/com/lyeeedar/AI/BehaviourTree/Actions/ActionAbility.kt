package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.AI.Tasks.TaskAbility
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.ability
import com.lyeeedar.Components.stats
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.randomOrNull
import ktx.collections.toGdxArray

class ActionAbility : AbstractAction()
{
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val target = getData<Point>(key, null)
		val posData = Mappers.position.get(entity)
		val taskData = Mappers.task.get(entity)
		val tile = posData.position as? Tile
		val ability = entity.ability()
		val stats = entity.stats()

		// doesnt have all the needed data, fail
		if ( target == null || posData == null || tile == null || taskData == null || ability == null || stats == null )
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

		for (ab in ability.abilities)
		{
			ab.remainingCooldown--
		}

		if (target.taxiDist(tile) > stats.attackDefinition.range)
		{
			state = ExecutionState.FAILED
			return state
		}

		val readyAbility = ability.abilities.filter { it.remainingCooldown <= 0 && it.condition.evaluate(stats.variables()) != 0f }.toGdxArray().randomOrNull()
		if (readyAbility != null)
		{
			if (readyAbility.singleUse)
			{
				readyAbility.remainingCooldown = Int.MAX_VALUE
			}
			else
			{
				readyAbility.remainingCooldown = readyAbility.cooldown.getValue()
			}
			readyAbility.selectedCooldown = readyAbility.remainingCooldown
			readyAbility.justUsed = true

			val newAb = readyAbility.ability.copy()
			newAb.source = entity
			taskData.tasks.add(TaskAbility(targetTile, newAb))

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