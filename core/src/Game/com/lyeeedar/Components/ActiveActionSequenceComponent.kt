package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Util.XmlData

fun Entity.activeActionSequence(): ActiveActionSequenceComponent? = ActiveActionSequenceComponent.mapper.get(this)
class ActiveActionSequenceComponent() : AbstractComponent()
{
	fun set(sequence: ActionSequence): ActiveActionSequenceComponent
	{
		this.sequence = sequence
		return this
	}

	lateinit var sequence: ActionSequence

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<ActiveActionSequenceComponent> = ComponentMapper.getFor(ActiveActionSequenceComponent::class.java)
		fun get(entity: Entity): ActiveActionSequenceComponent? = mapper.get(entity)

		private val pool: Pool<ActiveActionSequenceComponent> = object : Pool<ActiveActionSequenceComponent>() {
			override fun newObject(): ActiveActionSequenceComponent
			{
				return ActiveActionSequenceComponent()
			}

		}

		@JvmStatic fun obtain(): ActiveActionSequenceComponent
		{
			val obj = ActiveActionSequenceComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { ActiveActionSequenceComponent.pool.free(this); obtained = false } }
}