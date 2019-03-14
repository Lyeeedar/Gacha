package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.isAllies
import com.lyeeedar.Components.isEnemies
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.XmlData

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionGetAllVisible(): AbstractAction()
{
	enum class Type
	{
		TILES, ALLIES, ENEMIES
	}

	var type: Type = Type.TILES
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val pos = Mappers.position.get(entity)
		val tile = pos.position as? Tile ?: return ExecutionState.FAILED
		val stats = Mappers.stats.get(entity)

		val points = tile.level.grid

		if (type == Type.ALLIES)
		{
			val temp = com.badlogic.gdx.utils.Array<Entity>()
			for (point in points)
			{
				val etile = tile.level.getTile(point) ?: continue
				for (e in etile.contents)
				{
					val estats = Mappers.stats.get(e) ?: continue

					if (e != entity && estats.hp > 0 && entity.isAllies(e))
					{
						temp.add(e)
					}
				}
			}

			setData( key, temp );
			state = if (temp.size > 0) ExecutionState.COMPLETED else ExecutionState.FAILED;
		}
		else if (type == Type.ENEMIES)
		{
			val temp = com.badlogic.gdx.utils.Array<Entity>()
			for (point in points)
			{
				val etile = tile.level.getTile(point) ?: continue
				for (e in etile.contents)
				{
					val estats = Mappers.stats.get(e) ?: continue

					if (e != entity && estats.hp > 0 && entity.isEnemies(e))
					{
						temp.add(e)
					}
				}
			}

			setData( key, temp )
			state = if(temp.size > 0) ExecutionState.COMPLETED else ExecutionState.FAILED;
		}
		else
		{
			setData( key, points )
			state = ExecutionState.COMPLETED
		}


		return state
	}

	override fun parse(xml: XmlData) {
		key = xml.getAttribute("Key").toLowerCase()
		if (xml.getAttribute("Type", null) != null)
		{
			type = Type.valueOf(xml.getAttribute("Type").toUpperCase())
		}
	}

	override fun cancel(entity: Entity) {

	}

}