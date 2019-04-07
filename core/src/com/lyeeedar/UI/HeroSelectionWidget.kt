package com.lyeeedar.UI

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class HeroSelectionWidget(var entity: Entity) : Widget()
{
	val background = AssetManager.loadTextureRegion("Icons/Empty")!!
	val border = AssetManager.loadTextureRegion("GUI/RewardChanceBorder")!!
	val white = AssetManager.loadTextureRegion("white")!!

	val whiteColour = Colour.WHITE
	val greyOut = Colour(0f, 0f, 0f, 0.8f)
	val lightGray = Colour.LIGHT_GRAY
	val darkGray = Colour.DARK_GRAY

	val layout = GlyphLayout()
	val font = Global.skin.getFont("small")

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
			batch.draw(renderable.textures[0], x, y + 5f, width, height)
		}

		val stats = entity.stats()
		if (stats != null)
		{
			layout.setText(font, "Lv." + stats.level, Color.WHITE, Global.stage.width * 0.5f, Align.left, false)

			batch.setColor(0f, 0f, 0f, 0.3f)
			batch.draw(white, x + 2f, y, width - 4f, layout.height + 10f)

			batch.color = Color.WHITE
			batch.draw(stats.factionData!!.icon.currentTexture, x + 8f, y + 8f, layout.height, layout.height)
			font.draw(batch, layout, x + 13f + layout.height, y + 8f + layout.height)
		}

		batch.setColor(entity.stats()!!.ascension.colour)
		batch.draw(border, x, y, width, height)

		if (alreadyUsed)
		{
			batch.setColor(greyOut)
			batch.draw(white, x, y, width, height)
		}
	}
}