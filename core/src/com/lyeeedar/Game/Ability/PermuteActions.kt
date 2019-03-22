package com.lyeeedar.Game.Ability

import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.isAllies
import com.lyeeedar.Components.isEnemies
import com.lyeeedar.Components.tile
import com.lyeeedar.Game.Tile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.toHitPointArray
import ktx.collections.toGdxArray

class PermuteAction(ability: Ability) : AbstractAbilityAction(ability)
{
	val hitPoints = Array<Point>()

	override fun enter(): Boolean
	{
		val current = ability.targets.toGdxArray()
		ability.targets.clear()

		val mat = Matrix3()
		mat.setToRotation(ability.facing.angle)
		val vec = Vector3()

		val addedset = ObjectSet<Tile>()
		for (tile in current)
		{
			for (point in hitPoints)
			{
				vec.set(point.x.toFloat(), point.y.toFloat(), 0f)
				vec.mul(mat)

				val dx = Math.round(vec.x)
				val dy = Math.round(vec.y)

				val ntile = ability.level.getTile(tile, dx, dy) ?: continue
				if (!addedset.contains(ntile))
				{
					ability.targets.add(ntile)
					addedset.add(ntile)
				}
			}
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = PermuteAction(ability)
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

class SelectAlliesAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var count: Int = 1

	override fun enter(): Boolean
	{
		ability.targets.clear()
		val allies = Array<Tile>()

		for (tile in ability.level.grid)
		{
			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot] ?: continue
				if (entity.isAllies(ability.source))
				{
					allies.add(tile)
					break
				}
			}
		}

		for (i in 0 until count)
		{
			if (allies.size == 0)
			{
				break
			}

			val tile = allies.random()
			allies.removeValue(tile, true)

			ability.targets.add(tile)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = SelectAlliesAction(ability)
		action.count = count

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		count = xmlData.getInt("Count")
	}
}

class SelectEnemiesAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var count: Int = 1

	override fun enter(): Boolean
	{
		ability.targets.clear()
		val enemies = Array<Tile>()

		for (tile in ability.level.grid)
		{
			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot] ?: continue
				if (entity.isEnemies(ability.source))
				{
					enemies.add(tile)
					break
				}
			}
		}

		for (i in 0 until count)
		{
			if (enemies.size == 0)
			{
				break
			}

			val tile = enemies.random()
			enemies.removeValue(tile, true)

			ability.targets.add(tile)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = SelectEnemiesAction(ability)
		action.count = count

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		count = xmlData.getInt("Count")
	}
}

class SelectRandomAction(ability: Ability) : AbstractAbilityAction(ability)
{
	var count: Int = 1

	override fun enter(): Boolean
	{
		ability.targets.clear()
		val tiles = Array<Tile>()

		for (tile in ability.level.grid)
		{
			tiles.add(tile)
		}

		for (i in 0 until count)
		{
			if (tiles.size == 0)
			{
				break
			}

			val tile = tiles.random()
			tiles.removeValue(tile, true)

			ability.targets.add(tile)
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = SelectRandomAction(ability)
		action.count = count

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		count = xmlData.getInt("Count")
	}
}

class ClearSelectionAction(ability: Ability) : AbstractAbilityAction(ability)
{
	override fun enter(): Boolean
	{
		ability.targets.clear()
		ability.targets.add(ability.source.tile()!!)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = ClearSelectionAction(ability)
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}
}