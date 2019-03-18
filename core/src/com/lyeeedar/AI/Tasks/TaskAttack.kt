package com.lyeeedar.AI.Tasks

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.isAllies
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Components.tile
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.SpaceSlot

class TaskAttack(val tile: Tile) : AbstractTask()
{
	override fun execute(e: Entity)
	{
		e.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, Direction.Companion.getDirection(e.tile()!!, tile))
		for (slot in SpaceSlot.Values)
		{
			val entity = tile.contents[slot] ?: continue
			val stats = entity.stats() ?: continue

			if (e.isAllies(entity)) continue

			stats.dealDamage(1f, false)
		}
	}
}