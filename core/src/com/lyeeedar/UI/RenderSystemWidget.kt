package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Global
import com.lyeeedar.Systems.level
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point

class RenderSystemWidget : Widget()
{
	val white = AssetManager.loadTextureRegion("GUI/border")

	init
	{
		instance = this
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		Global.engine.render().doRender(batch, this)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		val level = Global.engine.level!!
		val tileSize = Global.engine.render()!!.tileSize

		val xp = x + (width / 2f) - ((level.width * tileSize) / 2f)

		return Vector2(xp + point.x * tileSize, y + point.y * tileSize)
	}

	companion object
	{
		lateinit var instance: RenderSystemWidget
	}
}