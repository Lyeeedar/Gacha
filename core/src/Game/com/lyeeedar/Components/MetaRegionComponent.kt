package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData
import ktx.collections.addAll

class MetaRegionComponent :  AbstractComponent()
{
	val keys = Array<String>(1)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		keys.addAll(xml.get("Key").toLowerCase().split(','))
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<MetaRegionComponent> = object : Pool<MetaRegionComponent>() {
			override fun newObject(): MetaRegionComponent
			{
				return MetaRegionComponent()
			}

		}

		@JvmStatic fun obtain(): MetaRegionComponent
		{
			val obj = MetaRegionComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { MetaRegionComponent.pool.free(this); obtained = false } }
}
