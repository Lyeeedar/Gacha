package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Tile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.toGdxArray

class PermuteAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	val hitPoints = Array<Point>(4)

	val mat = Matrix3()
	val vec = Vector3()
	val addedset = ObjectSet<Tile>()
	override fun enter(): Boolean
	{
		val current = sequence.targets.toGdxArray()
		sequence.targets.clear()
		sequence.lockedTargets.clear()

		mat.setToRotation(sequence.facing.angle)

		addedset.clear()
		for (tile in current)
		{
			for (point in hitPoints)
			{
				vec.set(point.x.toFloat(), point.y.toFloat(), 0f)
				vec.mul(mat)

				val dx = Math.round(vec.x)
				val dy = Math.round(vec.y)

				val ntile = sequence.level.getTile(tile, dx, dy) ?: continue
				if (!addedset.contains(ntile))
				{
					sequence.targets.add(ntile)
					addedset.add(ntile)
				}
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = PermuteAction(sequence)
		action.hitPoints.addAll(hitPoints)

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val hitPointsEl = xmlData.getChildByName("HitPoints")
		if (hitPointsEl != null) hitPoints.addAll(hitPointsEl.toHitPointArray())
		else hitPoints.add(Point(0, 0))
	}
}

class SelectEntitiesAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	enum class Mode
	{
		Allies,
		Enemies,
		Any
	}

	lateinit var mode: Mode
	lateinit var count: CompiledExpression
	lateinit var conditionString: String
	var compiledCondition: CompiledExpression? = null
	var minimum = true

	val entities = ObjectSet<Entity>()
	override fun enter(): Boolean
	{
		sequence.targets.clear()
		sequence.lockedTargets.clear()

		entities.clear()

		for (tile in sequence.level.grid)
		{
			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot] ?: continue

				if (mode == Mode.Any || (mode == Mode.Allies && entity.isAllies(sequence.source)) || (mode == Mode.Enemies && entity.isEnemies(sequence.source)))
				{
					entities.add(entity)
				}
			}
		}

		val sorted: List<Entity>
		if (conditionString == "random")
		{
			sorted = entities.sortedBy { Random.random() }
		}
		else
		{
			if (minimum)
			{
				sorted = entities.sortedBy { compiledCondition!!.evaluate(it.stats()!!.variables()) }
			}
			else
			{
				sorted = entities.sortedByDescending { compiledCondition!!.evaluate(it.stats()!!.variables()) }
			}
		}

		val count = count.evaluate(sequence.source.stats().variables()).round()
		for (i in 0 until count)
		{
			if (i == sorted.size)
			{
				break
			}

			val ally = sorted[i]
			sequence.targets.add(ally.tile())
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = SelectEntitiesAction(sequence)
		action.mode = mode
		action.count = count
		action.conditionString = conditionString
		action.compiledCondition = compiledCondition
		action.minimum = minimum

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		if (xmlData.name == "SelectAllies")
		{
			mode = Mode.Allies
		}
		else if (xmlData.name == "SelectEnemies")
		{
			mode = Mode.Enemies
		}
		else if (xmlData.name == "SelectEntities")
		{
			mode = Mode.Any
		}
		else
		{
			throw Exception("Unknown select action type '${xmlData.name}'!")
		}

		val countStr = xmlData.get("Count", "1")!!
		count = CompiledExpression(countStr, StatisticsComponent.getDefaultVariables())

		conditionString = xmlData.get("Condition", "random")!!
		if (conditionString != "random")
		{
			compiledCondition = CompiledExpression(conditionString, StatisticsComponent.getDefaultVariables())
		}
		minimum = xmlData.getBoolean("Minimum", true)
	}
}

class SelectTilesAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var count: CompiledExpression
	var emptyOnly = false

	val tiles = Array<Tile>(4)
	override fun enter(): Boolean
	{
		sequence.targets.clear()
		sequence.lockedTargets.clear()

		tiles.clear()

		for (tile in sequence.level.grid)
		{
			tiles.add(tile)
		}

		val count = count.evaluate(sequence.source.stats().variables()).round()
		for (i in 0 until count)
		{
			if (tiles.size == 0)
			{
				break
			}

			val tile = tiles.random()
			tiles.removeValue(tile, true)

			sequence.targets.add(tile)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = SelectTilesAction(sequence)
		action.count = count
		action.emptyOnly = emptyOnly

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val countStr = xmlData.get("Count", "1")!!
		count = CompiledExpression(countStr, StatisticsComponent.getDefaultVariables())

		emptyOnly = xmlData.getBoolean("EmptyOnly", false)
	}
}

class SelectSelfAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	override fun enter(): Boolean
	{
		sequence.targets.clear()
		sequence.lockedTargets.clear()

		sequence.targets.add(sequence.source.tile()!!)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = SelectSelfAction(sequence)
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}
}

class LockTargetsAction(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	override fun enter(): Boolean
	{
		sequence.lockedTargets.clear()

		for (target in sequence.targets)
		{
			val tile = sequence.level.getTile(target) ?: continue
			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot] ?: continue
				sequence.lockedTargets.add(entity)
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = LockTargetsAction(sequence)
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}
}