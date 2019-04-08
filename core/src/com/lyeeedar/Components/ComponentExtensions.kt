package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
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

fun Entity.pos() = Mappers.position.get(this)
fun Entity.tile() = Mappers.position.get(this)?.position as? Tile
fun Entity.stats() = Mappers.stats.get(this)
fun Entity.renderable() = Mappers.renderable.get(this)
fun Entity.additionalRenderable() = Mappers.additionalRenderable.get(this)
fun Entity.renderOffset() = this.renderable()?.renderable?.animation?.renderOffset(false)
fun Entity.task() = Mappers.task.get(this)
fun Entity.directionalSprite() = Mappers.directionalSprite.get(this)
fun Entity.name() = Mappers.name.get(this)
fun Entity.trailing() = Mappers.trailing.get(this)
fun Entity.event(): EventComponent
{
	var event = Mappers.event.get(this)
	if (event == null)
	{
		event = EventComponent()
		this.add(event)
	}

	return event
}
fun Entity.dialogue() = Mappers.dialogue.get(this)
fun Entity.occludes() = Mappers.occludes.get(this)
fun Entity.metaregion() = Mappers.metaregion.get(this)
fun Entity.loaddata() = Mappers.loaddata.get(this)
fun Entity.ability() = Mappers.ability.get(this)

fun <T: AbstractComponent> Entity.hasComponent(c: Class<T>) = this.getComponent(c) != null

class Mappers
{
	companion object
	{
		val name: ComponentMapper<NameComponent> = ComponentMapper.getFor(NameComponent::class.java)
		val position: ComponentMapper<PositionComponent> = ComponentMapper.getFor(PositionComponent::class.java)
		val renderable: ComponentMapper<RenderableComponent> = ComponentMapper.getFor(RenderableComponent::class.java)
		val additionalRenderable: ComponentMapper<AdditionalRenderableComponent> = ComponentMapper.getFor(AdditionalRenderableComponent::class.java)
		val task: ComponentMapper<TaskComponent> = ComponentMapper.getFor(TaskComponent::class.java)
		val occludes: ComponentMapper<OccludesComponent> = ComponentMapper.getFor(OccludesComponent::class.java)
		val stats: ComponentMapper<StatisticsComponent> = ComponentMapper.getFor(StatisticsComponent::class.java)
		val directionalSprite: ComponentMapper<DirectionalSpriteComponent> = ComponentMapper.getFor(DirectionalSpriteComponent::class.java)
		val event: ComponentMapper<EventComponent> = ComponentMapper.getFor(EventComponent::class.java)
		val trailing: ComponentMapper<TrailingEntityComponent> = ComponentMapper.getFor(TrailingEntityComponent::class.java)
		val dialogue: ComponentMapper<DialogueComponent> = ComponentMapper.getFor(DialogueComponent::class.java)
		val metaregion: ComponentMapper<MetaRegionComponent> = ComponentMapper.getFor(MetaRegionComponent::class.java)
		val loaddata: ComponentMapper<LoadDataComponent> = ComponentMapper.getFor(LoadDataComponent::class.java)
		val activeAbility: ComponentMapper<ActiveAbilityComponent> = ComponentMapper.getFor(ActiveAbilityComponent::class.java)
		val ability: ComponentMapper<AbilityComponent> = ComponentMapper.getFor(AbilityComponent::class.java)
		val transient: ComponentMapper<TransientComponent> = ComponentMapper.getFor(TransientComponent::class.java)
	}
}

class EntityLoader()
{
	companion object
	{
		val sharedRenderableMap = ObjectMap<Int, Renderable>()

		@JvmStatic fun load(path: String): Entity
		{
			val xml = getXml(path)

			val entity = if (xml.get("Extends", null) != null) load(xml.get("Extends")) else Entity()

			entity.add(LoadDataComponent(path, xml))

			val componentsEl = xml.getChildByName("Components") ?: return entity

			for (componentEl in componentsEl.children())
			{
				val component = when(componentEl.name.toUpperCase())
				{
					"ADDITIONALRENDERABLES" -> AdditionalRenderableComponent()
					"DIRECTIONALSPRITE" -> DirectionalSpriteComponent()
					"METAREGION" -> MetaRegionComponent()
					"NAME" -> NameComponent()
					"OCCLUDES" -> OccludesComponent()
					"POSITION" -> PositionComponent()
					"RENDERABLE" -> RenderableComponent()
					"STATISTICS" -> StatisticsComponent()
					"AI" -> TaskComponent()
					"ABILITY" -> AbilityComponent()
					"TRAILING" -> TrailingEntityComponent()

					else -> throw Exception("Unknown component type '" + componentEl.name + "'!")
				}

				component.fromLoad = true
				component.parse(componentEl, entity, path.directory())
				entity.add(component)
			}

			if (entity.name() == null)
			{
				val name = NameComponent(path)
				name.fromLoad = true
				entity.add(name)
			}

			return entity
		}
	}
}

fun Entity.isAllies(other: Entity): Boolean
{
	val thisStats = this.stats()
	val otherStats = other.stats()
	if (thisStats == null || otherStats == null) return false

	return thisStats.faction == otherStats.faction
}

fun Entity.isEnemies(other: Entity): Boolean
{
	val thisStats = this.stats()
	val otherStats = other.stats()
	if (thisStats == null || otherStats == null) return false

	return thisStats.faction != otherStats.faction
}

fun Renderable.addToEngine(point: Point, offset: Vector2 = Vector2())
{
	val pe = Entity()
	pe.add(TransientComponent())
	pe.add(RenderableComponent(this))
	val ppos = PositionComponent()
	ppos.slot = SpaceSlot.EFFECT

	ppos.size = min(this.size[0], this.size[1])
	ppos.position = point
	ppos.offset = offset

	pe.add(ppos)
	Global.engine.addEntity(pe)
}