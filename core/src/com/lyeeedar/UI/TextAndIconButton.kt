package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Renderables.Sprite.Sprite

class TextAndIconButton(val text: String, val icon: Sprite, val font: BitmapFont) : Widget()
{
	var background: TextureRegion? = null

	init
	{
		touchable = Touchable.enabled
	}

	override fun act(delta: Float)
	{
		icon.update(delta)
		super.act(delta)
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (background != null)
		{
			batch.draw(background!!, x, y, width, height)
		}

		batch.draw(icon.currentTexture, x, y, width, height)
		font.draw(batch, text, x, y + height * 0.3f, width, Align.center, false)

		super.draw(batch, parentAlpha)
	}
}