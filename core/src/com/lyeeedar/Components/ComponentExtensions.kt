package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.XmlReader
import com.lyeeedar.Game.Tile
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set

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
	}
}

class EntityLoader()
{
	companion object
	{
		val sharedRenderableMap = ObjectMap<Int, Renderable>()

		val files: ObjectMap<String, FileHandle> by lazy { loadFiles() }

		private fun loadFiles(): ObjectMap<String, FileHandle>
		{
			val rootPath = "Entities"
			val root = Gdx.files.internal(rootPath)
			//if (!root.exists()) root = Gdx.files.absolute(rootPath)

			println("Loading files from " + root.path())

			val out = ObjectMap<String, FileHandle>()

			for (f in root.list())
			{
				println(f.path())
				out[f.nameWithoutExtension().toUpperCase()] = f
			}

			return out
		}

		@JvmStatic fun load(path: String): Entity
		{
			val xml = getXml("Entities/$path.xml")
			val entity = load(xml)

			if (entity.name() == null)
			{
				val name = NameComponent(path)
				name.fromLoad = true
				entity.add(name)
			}

			return entity
		}

		@JvmStatic fun load(xml: XmlData): Entity
		{
			val entity = if (xml.get("Extends", null) != null) load(xml.get("Extends")) else Entity()

			entity.add(LoadDataComponent(xml))

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
					"TRAILING" -> TrailingEntityComponent()

					else -> throw Exception("Unknown component type '" + componentEl.name + "'!")
				}

				component.fromLoad = true
				component.parse(componentEl, entity)
				entity.add(component)
			}

			if (xml.getBoolean("IsPlayer", false))
			{
				val name = entity.name() ?: NameComponent("player")
				name.isPlayer = true
				name.fromLoad = true
				entity.add(name)
			}

			return entity
		}

		fun getSlot(xml: XmlReader.Element): SpaceSlot
		{
			var slot = SpaceSlot.ENTITY

			val extends = xml.get("Extends", null)
			if (extends != null)
			{
				val extendsxml = XmlReader().parse(files[extends.toUpperCase()])
				slot = getSlot(extendsxml)
			}

			val componentsEl = xml.getChildByName("Components")
			if (componentsEl != null)
			{
				val positionEl = componentsEl.getChildByName("Position")
				if (positionEl != null)
				{
					slot = SpaceSlot.valueOf(positionEl.get("SpaceSlot", "Entity").toUpperCase())
				}
			}

			return slot
		}
	}
}

fun Entity.isAllies(other: Entity): Boolean
{
	if (this.stats() == null || other.stats() == null) return false

	if (this.stats().factions.size == 0 || other.stats().factions.size == 0) return false

	return this.stats().factions.any { other.stats().factions.contains(it) }
}

fun Entity.isEnemies(other: Entity): Boolean
{
	if (this.stats() == null || other.stats() == null) return false

	if (this.stats().factions.size == 0 || other.stats().factions.size == 0) return false

	return !this.stats().factions.any { other.stats().factions.contains(it) }
}