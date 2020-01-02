package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

class DirectionalSpriteComponent() : AbstractComponent()
{
	lateinit var directionalSprite: DirectionalSprite

	var currentAnim: String = "idle"
	var lastV: DirectionalSprite.VDir = DirectionalSprite.VDir.DOWN
	var lastH: DirectionalSprite.HDir = DirectionalSprite.HDir.RIGHT

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		directionalSprite = AssetManager.loadDirectionalSprite(xml, entity.pos()?.size ?: 1)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DirectionalSpriteComponent> = object : Pool<DirectionalSpriteComponent>() {
			override fun newObject(): DirectionalSpriteComponent
			{
				return DirectionalSpriteComponent()
			}

		}

		@JvmStatic fun obtain(): DirectionalSpriteComponent
		{
			val obj = DirectionalSpriteComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { DirectionalSpriteComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		lastV = DirectionalSprite.VDir.DOWN
		lastH = DirectionalSprite.HDir.RIGHT
		currentAnim = "idle"
	}
}
