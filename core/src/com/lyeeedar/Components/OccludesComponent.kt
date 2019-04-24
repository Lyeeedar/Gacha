package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

class OccludesComponent : AbstractComponent()
{
	var occludes = true

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		occludes = xml.getBoolean("Occludes", true)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<OccludesComponent> = object : Pool<OccludesComponent>() {
			override fun newObject(): OccludesComponent
			{
				return OccludesComponent()
			}

		}

		@JvmStatic fun obtain(): OccludesComponent
		{
			val obj = OccludesComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { OccludesComponent.pool.free(this); obtained = false } }
}