package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

fun Entity.trailingEntity(): TrailingEntityComponent? = TrailingEntityComponent.mapper.get(this)
class TrailingEntityComponent : AbstractComponent()
{
	var initialised = false

	var collapses = true
	val entities = Array<Entity>(1)
	val tiles = Array<Tile>(1)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		collapses = xml.getBoolean("Collapses", true)
		entities.add(entity)

		val renderablesEl = xml.getChildByName("Renderables")!!
		for (el in renderablesEl.children())
		{
			val renderable = AssetManager.loadRenderable(el)
			val trailEntity = EntityPool.obtain()
			trailEntity.add(RenderableComponent.obtain().set(renderable))
			trailEntity.add(this)
			trailEntity.add(PositionComponent.obtain())
			trailEntity.pos()!!.moveable = false

			Future.call(
					{
						trailEntity.pos()!!.slot = entity.pos()!!.slot
						if (entity.stats() != null) trailEntity.add(entity.stats())
					}, 0f)

			entities.add(trailEntity)
		}
	}

	fun updatePos(tile: Tile)
	{
		var target = tile
		for (i in 0 until entities.size)
		{
			val entity = entities[i]
			entity.pos()!!.tile = target

			if (tiles.size <= i)
			{
				tiles.add(target)
				if (i > 0)
				{
					Global.engine.addEntity(entities[i])
				}
			}
			else
			{
				val prevTile = tiles[i]
				tiles[i] = target

				if (prevTile.contents[entity.pos()!!.slot] == entity) prevTile.contents[entity.pos()!!.slot] = null
				if (!target.contents.containsKey(entity.pos()!!.slot)) target.contents[entity.pos()!!.slot] = entity

				if (prevTile != target)
				{
					//entity.renderable().renderable.rotation = getRotation(prevTile, target)
					entity.renderable()!!.renderable.animation = MoveAnimation.obtain().set(target, prevTile, 0.15f)
				}
				else
				{
					if (!entities[0].renderable()!!.renderable.visible)
					{
						entity.renderable()!!.renderable.visible = false
					}
				}

				target = prevTile
			}
		}

		if (entities.all { !it.renderable()!!.renderable.visible })
		{
			entities.forEach { it.add(MarkedForDeletionComponent.obtain()) }
		}
	}

	override fun saveData(kryo: Kryo, output: Output)
	{
		output.writeInt(entities.size)

		for (i in 0..entities.size-1)
		{
			val tile = if (tiles.size == 0) entities[0].tile()!! else if (tiles.size <= i) tiles.last() else tiles[i]

			output.writeInt(tile.x)
			output.writeInt(tile.y)
		}
	}

	override fun loadData(kryo: Kryo, input: Input)
	{
		val count = input.readInt()
		if (count != entities.size) throw Exception("Mismatched count of trail! Expected '" + entities.size + "' but got '" + count + "'!")

		val points = Array<Point>(entities.size)

		for (i in 0 until entities.size)
		{
			val x = input.readInt()
			val y = input.readInt()

			points.add(Point(x, y))
		}

		Future.call({
			val head = entities[0]
			if (head.pos()!!.position != points[0]) System.err.println("Trail head isnt in the same place as it was saved!")

			for (i in 0 until points.size)
			{
				val entity = entities[i]
				val point = points[i]

				val tile = head.tile()!!.level.getTile(point)!!

				if (tiles.size == i) tiles.add(tile)
				else tiles[i] = tile

				entity.pos()!!.tile = tile

				tile.contents[entity.pos()!!.slot] = entity

				if (i > 0) Global.engine.addEntity(entities[i])
			}
		}, 0f)
	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<TrailingEntityComponent> = ComponentMapper.getFor(TrailingEntityComponent::class.java)
		fun get(entity: Entity): TrailingEntityComponent? = mapper.get(entity)

		private val pool: Pool<TrailingEntityComponent> = object : Pool<TrailingEntityComponent>() {
			override fun newObject(): TrailingEntityComponent
			{
				return TrailingEntityComponent()
			}

		}

		@JvmStatic fun obtain(): TrailingEntityComponent
		{
			val obj = TrailingEntityComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { TrailingEntityComponent.pool.free(this); obtained = false } }
}