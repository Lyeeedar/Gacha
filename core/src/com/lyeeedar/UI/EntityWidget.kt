package com.lyeeedar.UI

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.RenderSystem
import com.lyeeedar.Systems.RenderSystem.Companion.numHpPips
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.max

class EntityWidget(var entity: Entity) : Widget()
{
	val background = AssetManager.loadTextureRegion("Icons/Empty")!!
	val border = AssetManager.loadTextureRegion("GUI/border")!!
	val white = AssetManager.loadTextureRegion("white")!!
	val hp_border = AssetManager.loadTextureRegion("Sprites/GUI/health_border.png")!!

	val hpColour = Color.CYAN
	val healCol = Color.GREEN
	val lostHpCol = Color.ORANGE
	val emptyCol = Color.BLACK
	val greyOut = Color(0f, 0f, 0f, 0.6f)

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.color = Color.WHITE
		batch.draw(background, x, y, width, height)

		val renderable = entity.renderable()?.renderable as? Sprite

		if (renderable != null)
		{
			batch.draw(renderable.textures[0], x, y, width, height)
		}

		val stats = entity.stats()
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

			batch.color = emptyCol
			batch.draw(white, x+5f, y+5f, width, 5f)

			val lostLen = (hp + stats.lostHp) / maxhp
			batch.color = lostHpCol
			batch.draw(white, x+5f, y+5f, width*lostLen, 5f)

			val hpLen = hp / maxhp
			batch.color = hpColour
			batch.draw(white, x+5f, y+5f, width*hpLen, 5f)

			batch.color = Color.WHITE
			for (i in 0 until numHpPips)
			{
				batch.draw(hp_border, x+5f+i*spacePerPip, y+5f, solid, 5f)
			}

			for (i in 0 until stats.buffs.size)
			{
				val buff = stats.buffs[i]
				batch.draw(buff.icon.currentTexture, x+5f+i*solid*4, y+10f, solid*4, solid*4)
			}

			if (hp == 0f)
			{
				batch.color = greyOut
				batch.draw(white, x, y, width, height)

				batch.color = Color.DARK_GRAY
				batch.draw(border, x, y, width, height)
			}
			else
			{
				batch.color = Color.LIGHT_GRAY
				batch.draw(border, x, y, width, height)
			}
		}
	}
}