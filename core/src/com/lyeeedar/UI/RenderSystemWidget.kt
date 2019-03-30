package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
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

		touchable = Touchable.enabled

		addListener(object : InputListener()
					{
						override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
						{
							val level = Global.engine.level!!
							level.touchDown(screenspaceToPoint(x, y))

							super.touchUp(event, x, y, pointer, button)
							return true
						}

						override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
						{
							val level = Global.engine.level!!
							level.touchUp(screenspaceToPoint(x, y))

							super.touchUp(event, x, y, pointer, button)
						}

						override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int)
						{
							val level = Global.engine.level!!
							level.touchDragged(screenspaceToPoint(x, y))

							super.touchDragged(event, x, y, pointer)
						}

						override fun keyTyped(event: InputEvent?, character: Char): Boolean
						{
							return false
						}
					})
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		Global.engine.render().doRender(batch, this)
	}

	fun screenspaceToPoint(x: Float, y: Float): Point
	{
		val level = Global.engine.level!!
		val tileSize = Global.engine.render()!!.tileSize

		val xp = x + ((level.width * tileSize) / 2f) - (width / 2f)

		val sx = (xp / tileSize).toInt()
		val sy = (y / tileSize).toInt()

		return Point(sx, sy)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		val level = Global.engine.level!!
		val tileSize = Global.engine.render()!!.tileSize

		val xp = (width / 2f) - ((level.width * tileSize) / 2f)

		return this.localToStageCoordinates(Vector2(xp + point.x * tileSize, point.y * tileSize))
	}

	companion object
	{
		lateinit var instance: RenderSystemWidget
	}
}