package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.directory
import com.lyeeedar.Util.getXml
import com.lyeeedar.Util.min

fun Entity.tile() = PositionComponent.get(this)?.position as? Tile

class EntityLoader()
{
	companion object
	{
		val sharedRenderableMap = ObjectMap<Int, Renderable>()

		@JvmStatic fun load(path: String): Entity
		{
			val xml = getXml(path)

			val entity = if (xml.get("Extends", null) != null) load(xml.get("Extends")) else EntityPool.obtain()

			entity.add(LoadDataComponent.obtain().set(path, xml, true))

			val componentsEl = xml.getChildByName("Components") ?: return entity

			for (componentEl in componentsEl.children())
			{
				if (Global.resolveInstant)
				{
					if (
						componentEl.name.toUpperCase() == "ADDITIONALRENDERABLES" ||
						componentEl.name.toUpperCase() == "DIRECTIONALSPRITE" ||
						componentEl.name.toUpperCase() == "RENDERABLE")
					{
						continue
					}
				}

				val component = when(componentEl.name.toUpperCase())
				{
					"ADDITIONALRENDERABLES" -> AdditionalRenderableComponent.obtain()
					"DIRECTIONALSPRITE" -> DirectionalSpriteComponent.obtain()
					"METAREGION" -> MetaRegionComponent.obtain()
					"NAME" -> NameComponent.obtain()
					"OCCLUDES" -> OccludesComponent.obtain()
					"POSITION" -> PositionComponent.obtain()
					"RENDERABLE" -> RenderableComponent.obtain()
					"STATISTICS" -> StatisticsComponent.obtain()
					"AI" -> TaskComponent.obtain()
					"ABILITY" -> AbilityComponent.obtain()
					"TRAILING" -> TrailingEntityComponent.obtain()
					"EVENTHANDLER" -> EventHandlerComponent.obtain()

					else -> throw Exception("Unknown component type '" + componentEl.name + "'!")
				}

				component.fromLoad = true
				component.parse(componentEl, entity, path.directory())
				entity.add(component)
			}

			if (entity.name() == null)
			{
				val name = NameComponent.obtain()
				name.set(path)
				name.fromLoad = true
				entity.add(name)
			}

			return entity
		}
	}
}

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