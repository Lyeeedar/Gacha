package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.min

fun Entity.tile() = PositionComponent.get(this)?.position as? Tile

fun Renderable.addToEngine(point: Point, offset: Vector2 = Vector2())
{
	val pe = EntityPool.obtain()
	pe.add(TransientComponent.obtain())
	pe.add(RenderableComponent.obtain().set(this))
	val ppos = PositionComponent.obtain()
	ppos.slot = SpaceSlot.EFFECT

	ppos.size = min(this.size[0], this.size[1])
	ppos.position = point
	ppos.offset = offset

	pe.add(ppos)
	Global.engine.addEntity(pe)
}

fun loadGameComponents(name: String): AbstractComponent?
{
	return when(name)
	{
		"STATISTICS" -> StatisticsComponent.obtain()
		"AI" -> TaskComponent.obtain()
		"ABILITY" -> AbilityComponent.obtain()
		"TRAILING" -> TrailingEntityComponent.obtain()
		"EVENTHANDLER" -> EventHandlerComponent.obtain()
		else -> null
	}
}