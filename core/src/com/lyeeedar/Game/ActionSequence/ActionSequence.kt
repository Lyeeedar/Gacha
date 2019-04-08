package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.pos
import com.lyeeedar.Direction
import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.addAll

class ActionSequence
{
	val actions: Array<AbstractActionSequenceAction> = Array()

	val enteredActions: Array<AbstractActionSequenceAction> = Array(false, 4)
	lateinit var source: Entity
	val targets: Array<Point> = Array()
	val lockedTargets: Array<Entity> = Array()
	lateinit var level: Level
	lateinit var facing: Direction

	var blocked = false
	var currentTime: Float = 0f
	var index = 0

	fun begin(source: Entity, level: Level)
	{
		if (enteredActions.size > 0)
		{
			throw Exception("Tried to start a non-complete sequence!")
		}

		this.level = level
		this.source = source
		lockedTargets.clear()
		targets.clear()
		enteredActions.clear()
		index = 0
		currentTime = 0f
		blocked = false
		facing = Direction.NORTH

		targets.add(source.pos()!!.position)
	}

	fun onTurn()
	{
		var anyBlocked = false
		for (action in enteredActions)
		{
			val stillBlocked = action.onTurn()
			if (stillBlocked)
			{
				anyBlocked = true
			}
		}
		blocked = anyBlocked
	}

	fun update(delta: Float): Boolean
	{
		if (blocked)
		{
			return false
		}

		if (lockedTargets.size > 0)
		{
			targets.clear()
			for (target in lockedTargets)
			{
				targets.add(target.pos().position)
			}
		}

		if (Global.resolveInstant)
		{
			currentTime = actions.last().end * 2f
		}

		currentTime += delta

		val itr = enteredActions.iterator()
		while (itr.hasNext())
		{
			val action = itr.next()
			if (action.end <= currentTime)
			{
				action.exit()
				itr.remove()
			}
		}

		if (index >= actions.size)
		{
			if (enteredActions.size == 0)
			{
				blocked = false
				return true
			}
		}

		for (i in index until actions.size)
		{
			val action = actions[i]
			if (action.start <= currentTime)
			{
				index = i + 1

				val blocked = action.enter()
				if (blocked)
				{
					this.blocked = blocked
					enteredActions.add(action)
					break
				}
				else
				{
					if (action.end <= currentTime)
					{
						action.exit()
					}
					else
					{
						enteredActions.add(action)
					}
				}
			}
			else
			{
				index = i
				break
			}
		}

		return false
	}

	fun cancel()
	{
		for (action in enteredActions)
		{
			action.exit()
		}
		enteredActions.clear()
		blocked = false
		index = 0
		currentTime = 0f
		targets.clear()
	}

	fun copy(): ActionSequence
	{
		val ability = ActionSequence()
		for (action in actions)
		{
			val copy = action.copy(ability)
			ability.actions.add(copy)
		}

		return ability
	}

	companion object
	{
		fun load(xmlData: XmlData): ActionSequence
		{
			val actions = Array<AbstractActionSequenceAction>()
			val ability = ActionSequence()

			for (timelineEl in xmlData.children)
			{
				for (actionEl in timelineEl.children)
				{
					val action = AbstractActionSequenceAction.load(actionEl, ability)
					actions.add(action)
				}
			}

			val sorted = actions.sortedBy { it.start }
			ability.actions.addAll(sorted)

			return ability
		}
	}
}