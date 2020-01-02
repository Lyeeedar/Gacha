package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

class MarkedForDeletionComponent : AbstractComponent()
{
	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<MarkedForDeletionComponent> = object : Pool<MarkedForDeletionComponent>() {
			override fun newObject(): MarkedForDeletionComponent
			{
				return MarkedForDeletionComponent()
			}

		}

		@JvmStatic fun obtain(): MarkedForDeletionComponent
		{
			val obj = MarkedForDeletionComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { MarkedForDeletionComponent.pool.free(this); obtained = false } }
}