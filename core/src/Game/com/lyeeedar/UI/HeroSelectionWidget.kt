package com.lyeeedar.UI

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Components.renderableOrNull
import com.lyeeedar.Components.stats
import com.lyeeedar.Components.statsOrNull
import com.lyeeedar.Game.EntityData
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics

class HeroSelectionWidget(var entity: Entity, val heroData: EntityData, val showIcons: Boolean = false) : Widget()
{
	val background = AssetManager.loadTextureRegion("Icons/Empty")!!
	val border = AssetManager.loadTextureRegion("GUI/RewardChanceBorder")!!
	val white = AssetManager.loadTextureRegion("white")!!

	val redColour = Colour(0.85f, 0f, 0f, 1f)
	val whiteColour = Colour.WHITE
	val greyOut = Colour(0f, 0f, 0f, 0.8f)
	val lightGray = Colour.LIGHT_GRAY
	val darkGray = Colour.DARK_GRAY

	val layout = GlyphLayout()
	val font = Statics.skin.getFont("small")
	val bigfont = Statics.skin.getFont("default")

	val borderedCircle = AssetManager.loadTextureRegion("borderedcircle")!!
	val shard = AssetManager.loadTextureRegion("Icons/shard_notification")!!
	val armour = AssetManager.loadTextureRegion("Icons/armour_notification")!!

	var alreadyUsed = false

	init
	{
		touchable = Touchable.enabled
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.setColor(whiteColour)
		batch.draw(background, x, y, width, height)

		val renderable = entity.renderableOrNull()?.renderable as? Sprite

		if (renderable != null)
		{
			batch.draw(renderable.textures[0], x, y + 5f, width, height)
		}

		val stats = entity.statsOrNull()
		if (stats != null)
		{
			layout.setText(font, "Lv." + stats.level, Color.WHITE, Statics.stage.width * 0.5f, Align.left, false)

			batch.setColor(0f, 0f, 0f, 0.3f)
			batch.draw(white, x + 2f, y, width - 4f, layout.height + 10f)

			batch.color = Color.WHITE
			batch.draw(stats.factionData!!.icon.currentTexture, x + 8f, y + 8f, layout.height, layout.height)
			font.draw(batch, layout, x + 13f + layout.height, y + 8f + layout.height)
		}

		batch.color = Color.WHITE
		layout.setText(font, heroData.factionEntity.rarity.shortName, heroData.factionEntity.rarity.colour.color(), Statics.stage.width * 0.5f, Align.left, false)
		font.draw(batch, layout, x + width - layout.width - 5f, y + height - layout.height)

		batch.setColor(entity.stats().ascension.colour)
		batch.draw(border, x, y, width, height)

		if (showIcons)
		{
			var y = y + height - 7f - 8f
			if (heroData.ascensionShards >= heroData.ascension.nextAscension.shardsRequired)
			{
				batch.setColor(redColour)
				batch.draw(borderedCircle, x + 7f, y, 8f, 8f)
				batch.color = Color.WHITE
				batch.draw(shard, x + 7f, y, 8f, 8f)

				y -= 11f
			}

			if (stats != null && !stats.hasBestEquipment(entity))
			{
				batch.setColor(redColour)
				batch.draw(borderedCircle, x + 7f, y, 8f, 8f)
				batch.color = Color.WHITE
				batch.draw(armour, x + 7f, y, 8f, 8f)

				y -= 11f
			}
		}

		if (alreadyUsed)
		{
			batch.setColor(greyOut)
			batch.draw(white, x, y, width, height)
		}
	}
}