package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.pos
import com.lyeeedar.Direction
import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class ActionSequence
{
	var creatingName = ""
	var creatingIcon: Sprite? = null

	var cancellable = true
	var removeOnDeath = false
	val actions: Array<AbstractActionSequenceAction> = Array(4)

	val enteredActions: Array<AbstractActionSequenceAction> = Array(false, 4)
	lateinit var source: Entity
	val targets: Array<Point> = Array(1)
	val lockedTargets: Array<Entity> = Array(1)
	val storedTargets = ObjectMap<String, Array<Point>>()
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
				val pos = target.pos() ?: continue
				targets.add(pos.position)
			}
		}

		if (Global.resolveInstant)
		{
			currentTime = (actions.lastOrNull()?.end ?: 0f) * 2f
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
					currentTime = action.start
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
		val sequence = ActionSequence.obtain()
		for (action in actions)
		{
			val copy = action.copy(sequence)
			sequence.actions.add(copy)
		}
		sequence.cancellable = cancellable

		return sequence
	}

	var obtained: Boolean = false
	companion object
	{
		val loadedSequences = ObjectMap<XmlData, ActionSequence>()
		fun load(xmlData: XmlData): ActionSequence
		{
			val existing = loadedSequences[xmlData]
			if (existing != null) return existing.copy()

			val actions = Array<AbstractActionSequenceAction>(4)
			val sequence = ActionSequence.obtain()

			for (timelineEl in xmlData.children)
			{
				for (actionEl in timelineEl.children)
				{
					val action = AbstractActionSequenceAction.load(actionEl, sequence)
					actions.add(action)
				}
			}

			val sorted = actions.sortedBy { it.start }

			var i = 0
			while (i < sorted.size)
			{
				val currentAction = sorted[i]

				if (currentAction is RepeatBegin)
				{
					// search forward to find the end, store items in list
					val actionsToRepeat = Array<AbstractActionSequenceAction>()
					var endI = i+1

					while (endI < sorted.size)
					{
						val endAction = sorted[endI]

						if (endAction is RepeatBegin)
						{
							throw Exception("Cannot nest Repeat blocks!")
						}
						else if (endAction is RepeatEnd)
						{
							break
						}
						else
						{
							actionsToRepeat.add(endAction)
						}

						endI++
					}

					if (endI == sorted.size)
					{
						throw Exception("Repeat block must have a RepeatEnd!")
					}

					val start = currentAction.start
					val end = sorted[endI].end

					// for num count, copy items and place into array
					var currentTime = start
					for (repeatNum in 0 until currentAction.count)
					{
						for (action in actionsToRepeat)
						{
							val offset = action.start - start
							val newStart = currentTime + offset
							val newEnd = newStart + (action.end - action.start)

							val newAction = action.copy(sequence)
							newAction.start = newStart
							newAction.end = newEnd

							sequence.actions.add(newAction)
						}

						currentTime += end - start
					}

					i = endI+1

					val totalOffset = currentTime - end

					// move all remaining actions along
					for (i in 0 until sorted.size)
					{
						val action = sorted[i]

						if (action.start >= end)
						{
							action.start += totalOffset
						}

						if (action.end >= end)
						{
							action.end += totalOffset
						}
					}
				}
				else
				{
					sequence.actions.add(currentAction)
				}

				i++
			}

			loadedSequences[xmlData] = sequence

			return sequence
		}

		private val pool: Pool<ActionSequence> = object : Pool<ActionSequence>() {
			override fun newObject(): ActionSequence
			{
				return ActionSequence()
			}

		}

		@JvmStatic fun obtain(): ActionSequence
		{
			val obj = ActionSequence.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free()
	{
		cancel()

		for (action in actions)
		{
			action.free()
		}
		actions.clear()

		if (obtained)
		{
			ActionSequence.pool.free(this);
			obtained = false
		}
	}
}