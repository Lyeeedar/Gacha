package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.isAllies
import com.lyeeedar.Components.occludes
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.trailingEntity
import com.lyeeedar.Direction
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Point

class Tile(x: Int = 0, y: Int = 0) : Point(x, y), IPathfindingTile
{
	lateinit var level: Level

	val contents: FastEnumMap<SpaceSlot, Entity> = FastEnumMap( SpaceSlot::class.java )

	val neighbours: FastEnumMap<Direction, Tile> = FastEnumMap(Direction::class.java)

	var sprite: SpriteWrapper? = null

	var isValidTarget = false
	var isValidHitPoint = false
	var isSelectedPoint = false

	init
	{
		sprite = SpriteWrapper()
		sprite!!.sprite = AssetManager.loadSprite("white", colour = Colour.LIGHT_GRAY)
	}

	fun firstEntity(): Entity?
	{
		for (slot in SpaceSlot.EntityValues)
		{
			val entity = contents[slot] ?: continue
			return entity
		}

		return null
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?) = 0

	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		if (travelType == SpaceSlot.LIGHT)
		{
			for (slot in SpaceSlot.Values)
			{
				val obj = contents.get(slot)
				if (obj != null && obj != self)
				{
					val occludes = obj.occludes()
					if (occludes != null)
					{
						if (occludes.occludes) return false
					}
					else if (slot == SpaceSlot.WALL)
					{
						return false
					}
				}
			}

			return true
		}
		else
		{
			if (contents.get(SpaceSlot.WALL) != null) { return false; }

			val obj = contents.get(travelType)
			if (obj != null && obj != self)
			{
				if (self is Entity)
				{
					if (self.isAllies(obj) && obj.pos().turnsOnTile < 1)
					{
						return true
					}
					else if (self.trailingEntity()?.entities?.contains(obj) == true)
					{
						return true
					}
				}

				return false
			}

			return true
		}
	}
}