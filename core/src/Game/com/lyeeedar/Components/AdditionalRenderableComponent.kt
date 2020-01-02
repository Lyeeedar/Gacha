package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class AdditionalRenderableComponent : AbstractComponent()
{
	val below = ObjectMap<String, Renderable>()
	val above = ObjectMap<String, Renderable>()

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val pos = entity.pos()

		val belowEls = xml.getChildByName("Below")
		if (belowEls != null)
		{
			for (el in belowEls.children())
			{
				val key = el.get("Key")
				val renderable = AssetManager.loadRenderable(el.getChildByName("Renderable")!!)

				if (pos != null)
				{
					renderable.size[0] = pos.size
					renderable.size[1] = pos.size
				}

				below[key] = renderable
			}
		}

		val aboveEls = xml.getChildByName("Above")
		if (aboveEls != null)
		{
			for (el in aboveEls.children())
			{
				val key = el.get("Key")
				val renderable = AssetManager.loadRenderable(el.getChildByName("Renderable")!!)

				if (pos != null)
				{
					renderable.size[0] = pos.size
					renderable.size[1] = pos.size
				}

				above[key] = renderable
			}
		}
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<AdditionalRenderableComponent> = object : Pool<AdditionalRenderableComponent>() {
			override fun newObject(): AdditionalRenderableComponent
			{
				return AdditionalRenderableComponent()
			}

		}

		@JvmStatic fun obtain(): AdditionalRenderableComponent
		{
			val obj = AdditionalRenderableComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { AdditionalRenderableComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		below.clear()
		above.clear()
	}
}
