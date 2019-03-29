package com.lyeeedar.UI

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Components.renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class HeroSelectionWidget(var entity: Entity) : Widget()
{
	val background = AssetManager.loadTextureRegion("Icons/Empty")!!
	val border = AssetManager.loadTextureRegion("GUI/border")!!
	val white = AssetManager.loadTextureRegion("white")!!

	val whiteColour = Colour.WHITE
	val greyOut = Colour(0f, 0f, 0f, 0.8f)
	val lightGray = Colour.LIGHT_GRAY
	val darkGray = Colour.DARK_GRAY

	var alreadyUsed = false

	init
	{
		touchable = Touchable.enabled
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.setColor(whiteColour)
		batch.draw(background, x, y, width, height)

		val renderable = entity.renderable()?.renderable as? Sprite

		if (renderable != null)
		{
			batch.draw(renderable.textures[0], x, y, width, height)
		}

		if (alreadyUsed)
		{
			batch.setColor(greyOut)
			batch.draw(white, x, y, width, height)

			batch.setColor(darkGray)
			batch.draw(border, x, y, width, height)
		}
		else
		{
			batch.setColor(lightGray)
			batch.draw(border, x, y, width, height)
		}
	}
}