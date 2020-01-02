package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Tile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

class PermuteAction() : AbstractActionSequenceAction()
{
	var appendTargets = false
	val hitPoints = Array<Point>(4)

	val mat = Matrix3()
	val vec = Vector3()
	val addedset = ObjectSet<Tile>()
	override fun enter(): Boolean
	{
		val current = sequence.targets.toGdxArray()

		if (!appendTargets)
		{
			sequence.targets.clear()
		}
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = PermuteAction.obtain()
		action.hitPoints.addAll(hitPoints)
		action.appendTargets = appendTargets

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		val hitPointsEl = xmlData.getChildByName("HitPoints")
		if (hitPointsEl != null) hitPoints.addAll(hitPointsEl.toHitPointArray())
		else hitPoints.add(Point(0, 0))

		appendTargets = xmlData.getBoolean("AppendTargets", false)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<PermuteAction> = object : Pool<PermuteAction>() {
			override fun newObject(): PermuteAction
			{
				return PermuteAction()
			}

		}

		@JvmStatic fun obtain(): PermuteAction
		{
			val obj = PermuteAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.hitPoints.clear()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { PermuteAction.pool.free(this); obtained = false } }
}

class SelectEntitiesAction() : AbstractActionSequenceAction()
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
	var allowSelf = true
	var allowCurrent = true

	val oldTargetsStore = ObjectSet<Point>()
	val entities = ObjectSet<Entity>()
	override fun enter(): Boolean
	{
		val oldTarget = sequence.targets.firstOrNull() ?: sequence.source.tile()!!

		oldTargetsStore.clear()
		oldTargetsStore.addAll(sequence.targets)

		sequence.targets.clear()
		sequence.lockedTargets.clear()

		entities.clear()

		for (tile in sequence.level.grid)
		{
			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot] ?: continue

				if (mode == Mode.Any ||
					(mode == Mode.Allies && entity.isAllies(sequence.source)) ||
					(mode == Mode.Enemies && entity.isEnemies(sequence.source)))
				{
					if (!allowSelf && entity == sequence.source)
					{

					}
					else if (!allowCurrent && oldTargetsStore.contains(tile))
					{

					}
					else
					{
						entities.add(entity)
					}
				}
			}
		}

		val sorted: List<Entity>
		if (conditionString == "random")
		{
			sorted = entities.sortedBy { Random.random() }
		}
		else if (conditionString == "dist")
		{
			sorted = entities.filter { it != sequence.source }.sortedBy { it.pos().position.dist(sequence.source.pos().position) }
		}
		else if (conditionString == "distcurrent")
		{
			sorted = entities.filter { it != oldTarget }.sortedBy { it.pos().position.dist(oldTarget) }
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = SelectEntitiesAction.obtain()
		action.mode = mode
		action.count = count
		action.conditionString = conditionString
		action.compiledCondition = compiledCondition
		action.minimum = minimum
		action.allowSelf = allowSelf
		action.allowCurrent = allowCurrent

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
		allowSelf = xmlData.getBoolean("AllowSelf", true)
		allowCurrent = xmlData.getBoolean("AllowCurrent", true)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SelectEntitiesAction> = object : Pool<SelectEntitiesAction>() {
			override fun newObject(): SelectEntitiesAction
			{
				return SelectEntitiesAction()
			}

		}

		@JvmStatic fun obtain(): SelectEntitiesAction
		{
			val obj = SelectEntitiesAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SelectEntitiesAction.pool.free(this); obtained = false } }
}

class SelectTilesAction() : AbstractActionSequenceAction()
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = SelectTilesAction.obtain()
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

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SelectTilesAction> = object : Pool<SelectTilesAction>() {
			override fun newObject(): SelectTilesAction
			{
				return SelectTilesAction()
			}

		}

		@JvmStatic fun obtain(): SelectTilesAction
		{
			val obj = SelectTilesAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SelectTilesAction.pool.free(this); obtained = false } }
}

class SelectSelfAction() : AbstractActionSequenceAction()
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = SelectSelfAction.obtain()
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SelectSelfAction> = object : Pool<SelectSelfAction>() {
			override fun newObject(): SelectSelfAction
			{
				return SelectSelfAction()
			}

		}

		@JvmStatic fun obtain(): SelectSelfAction
		{
			val obj = SelectSelfAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SelectSelfAction.pool.free(this); obtained = false } }
}

class LockTargetsAction() : AbstractActionSequenceAction()
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = LockTargetsAction.obtain()
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<LockTargetsAction> = object : Pool<LockTargetsAction>() {
			override fun newObject(): LockTargetsAction
			{
				return LockTargetsAction()
			}

		}

		@JvmStatic fun obtain(): LockTargetsAction
		{
			val obj = LockTargetsAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { LockTargetsAction.pool.free(this); obtained = false } }
}

class StoreTargets() : AbstractActionSequenceAction()
{
	lateinit var key: String

	override fun enter(): Boolean
	{
		val targetCopy = sequence.targets.toGdxArray()
		sequence.storedTargets[key] = targetCopy

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = StoreTargets.obtain()
		action.key = key
		return action
	}

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<StoreTargets> = object : Pool<StoreTargets>() {
			override fun newObject(): StoreTargets
			{
				return StoreTargets()
			}

		}

		@JvmStatic fun obtain(): StoreTargets
		{
			val obj = StoreTargets.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { StoreTargets.pool.free(this); obtained = false } }
}

class RestoreTargets() : AbstractActionSequenceAction()
{
	lateinit var key: String

	override fun enter(): Boolean
	{
		val targets = sequence.storedTargets[key]
		sequence.targets.clear()
		sequence.targets.addAll(targets)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = RestoreTargets.obtain()
		action.key = key
		return action
	}

	override fun parse(xmlData: XmlData)
	{
		key = xmlData.get("Key")
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<RestoreTargets> = object : Pool<RestoreTargets>() {
			override fun newObject(): RestoreTargets
			{
				return RestoreTargets()
			}

		}

		@JvmStatic fun obtain(): RestoreTargets
		{
			val obj = RestoreTargets.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { RestoreTargets.pool.free(this); obtained = false } }
}