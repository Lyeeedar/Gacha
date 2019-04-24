package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Direction
import com.lyeeedar.Game.Tile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

class PositionComponent(): AbstractComponent()
{
	var position: Point = Point(-1, -1) // bottom left pos
		set(value)
		{
			if (value != field)
			{
				facing = Direction.getCardinalDirection(value.x - field.x, value.y - field.y)

				field = value
				max = value
				turnsOnTile = 0
			}
		}

	var tile: Tile?
		get() = position as? Tile
		set(value)
		{
			if (value != null) position = value
		}

	var min: Point
		set(value) { position = value }
		get() { return position }

	var offset: Vector2 = Vector2()

	var max: Point = Point(-1, -1)

	var size: Int = 1

	var slot: SpaceSlot = SpaceSlot.ENTITY

	var moveable = true

	var moveLocked = false

	var canFall = true

	var facing: Direction = Direction.SOUTH

	var turnsOnTile: Int = 0

	val x: Int
		get() = position.x
	val y: Int
		get() = position.y

	val tiles: Iterable<Tile>
		get() = (min.x..max.x).zip(min.y..max.y).mapNotNull { tile!!.level.getTile(it.first, it.second) }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val slotEl = xml.get("SpaceSlot", null)
		if (slotEl != null) slot = SpaceSlot.valueOf(slotEl.toUpperCase())

		canFall = xml.getBoolean("CanFall", true)
		moveable = xml.getBoolean("Moveable", true)

		size = xml.getInt("Size", 1)
		if (size != -1)
		{
			val renderable = entity.renderable()
			if (renderable != null)
			{
				renderable.renderable.size[0] = size
				renderable.renderable.size[1] = size
			}

			val directional = entity.directionalSprite()
			if (directional != null)
			{
				directional.directionalSprite.size = size
			}

			val additional = entity.additionalRenderable()
			if (additional != null)
			{
				for (r in additional.below.values())
				{
					r.size[0] = size
					r.size[1] = size
				}

				for (r in additional.above.values())
				{
					r.size[0] = size
					r.size[1] = size
				}
			}
		}
	}

	fun isOnTile(point: Point): Boolean
	{
		val tile = this.tile ?: return false

		for (x in 0 until size)
		{
			for (y in 0 until size)
			{
				val t = tile.level.getTile(tile, x, y) ?: continue
				if (t == point) return true
			}
		}
		return false
	}

	fun getEdgeTiles(dir: Direction): com.badlogic.gdx.utils.Array<Tile>
	{
		val tile = position as? Tile ?: throw Exception("Position must be a tile!")

		var xstep = 0
		var ystep = 0

		var sx = 0
		var sy = 0

		if ( dir == Direction.NORTH )
		{
			sx = 0
			sy = size - 1

			xstep = 1
			ystep = 0
		}
		else if ( dir == Direction.SOUTH )
		{
			sx = 0
			sy = 0

			xstep = 1
			ystep = 0
		}
		else if ( dir == Direction.EAST )
		{
			sx = size - 1
			sy = 0

			xstep = 0
			ystep = 1
		}
		else if ( dir == Direction.WEST )
		{
			sx = 0
			sy = 0

			xstep = 0
			ystep = 1
		}

		val tiles = com.badlogic.gdx.utils.Array<Tile>(1)
		for (i in 0 until size)
		{
			val t = tile.level.getTile(tile, sx + xstep * i, sy + ystep * i) ?: continue
			tiles.add(t)
		}

		return tiles
	}

	fun isValidTile(t: Tile, entity: Entity): Boolean
	{
		for (x in 0 until size)
		{
			for (y in 0 until size)
			{
				val tile = t.level.getTile(t, x, y)
				if (tile == null || (tile.contents.get(slot) != null && tile.contents.get(slot) != entity) || tile.contents.get(SpaceSlot.WALL) != null)
				{
					return false
				}
			}
		}

		return true
	}

	fun removeFromTile(entity: Entity)
	{
		if (tile == null) return

		for (x in 0 until size)
		{
			for (y in 0 until size)
			{
				val tile = tile!!.level.getTile(tile!!, x, y) ?: continue
				if (tile.contents[slot] == entity) tile.contents.remove(slot)
			}
		}
	}

	fun addToTile(entity: Entity)
	{
		val t = tile!!
		for (x in 0 until size)
		{
			for (y in 0 until size)
			{
				val tile = t.level.getTile(t, x, y) ?: continue
				tile.contents[slot] = entity
			}
		}
	}

	fun doMove(t: Tile, entity: Entity)
	{
		removeFromTile(entity)

		position = t

		addToTile(entity)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<PositionComponent> = object : Pool<PositionComponent>() {
			override fun newObject(): PositionComponent
			{
				return PositionComponent()
			}

		}

		@JvmStatic fun obtain(): PositionComponent
		{
			val obj = PositionComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { PositionComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		offset.set(0f, 0f)
		turnsOnTile = 0
		moveLocked = false
		facing = Direction.SOUTH
		position = Point(-1, -1)
		slot = SpaceSlot.ENTITY
		size = 1
		max = Point(-1, -1)
		moveable = true
		canFall = true
	}
}