package com.lyeeedar.UI

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Components.ability
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.RenderSystem
import com.lyeeedar.Systems.RenderSystem.Companion.numHpPips
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.max

class EntityWidget(var entity: Entity?) : Widget()
{
	val background = AssetManager.loadTextureRegion("Icons/Empty")!!
	val border = AssetManager.loadTextureRegion("GUI/border")!!
	val white = AssetManager.loadTextureRegion("white")!!
	val hp_border = AssetManager.loadTextureRegion("Sprites/GUI/health_border.png")!!

	val whiteColour = Colour.WHITE
	val hpColour = Colour.GREEN
	val healCol = Colour.GREEN
	val lostHpCol = Colour.ORANGE
	val emptyCol = Colour.BLACK
	val greyOut = Colour(0f, 0f, 0f, 0.8f)
	val lightGray = Colour.LIGHT_GRAY
	val darkGray = Colour.DARK_GRAY
	val gold = Colour.GOLD

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.setColor(whiteColour)
		batch.draw(background, x, y, width, height)

		val renderable = entity?.renderable()?.renderable as? Sprite
		if (renderable != null)
		{
			batch.draw(renderable.textures[0], x, y, width, height)
		}

		val stats = entity?.stats()
		if (stats != null)
		{
			val totalWidth = width-10f

			val hp = max(0f, stats.hp)
			val maxhp = stats.getStat(Statistic.MAXHP)

			val solidSpaceRatio = 0.05f
			val space = totalWidth
			val spacePerPip = space / RenderSystem.numHpPips
			val spacing = spacePerPip * solidSpaceRatio
			val solid = spacePerPip - spacing

			batch.setColor(emptyCol)
			batch.draw(white, x+5f, y+5f, totalWidth, 5f)

			val lostLen = (hp + stats.lostHp) / maxhp
			batch.setColor(lostHpCol)
			batch.draw(white, x+5f, y+5f, totalWidth*lostLen, 5f)

			val hpLen = hp / maxhp
			batch.setColor(hpColour)
			batch.draw(white, x+5f, y+5f, totalWidth*hpLen, 5f)

			batch.setColor(whiteColour)
			for (i in 0 until numHpPips)
			{
				batch.draw(hp_border, x+5f+i*spacePerPip, y+5f, solid, 5f)
			}

			for (i in 0 until stats.buffs.size)
			{
				val buff = stats.buffs[i]

				val icon = buff.icon ?: continue
				batch.draw(icon.currentTexture, x+5f+i*solid*4, y+10f, solid*4, solid*4)
			}

			val ability = entity!!.ability()
			if (ability != null)
			{
				val abx = x+width-17f
				val aby = y+height-17f

				batch.setColor(whiteColour)
				for (i in 0 until ability.abilities.size)
				{
					val ab = ability.abilities[i]
					batch.draw(background, abx, aby-i*12f, 10f, 10f)
					batch.draw(ab.icon.currentTexture, abx, aby-i*12f, 10f, 10f)

					if (ab.remainingCooldown > 0)
					{
						val alpha = ab.remainingCooldown.toFloat() / ab.selectedCooldown.toFloat()
						batch.setColor(greyOut)
						batch.draw(white, abx, aby-i*12f, 10f, 10f * alpha)

						batch.setColor(lightGray)
					}
					else
					{
						batch.setColor(gold)
					}

					batch.draw(border, abx, aby-i*12f, 10f, 10f)
				}
			}

			batch.setColor(stats.ascension.colour)
			batch.draw(border, x, y, width, height)

			if (hp == 0f)
			{
				batch.setColor(greyOut)
				batch.draw(white, x, y, width, height)
			}
		}
	}
}