package com.lyeeedar.UI

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Game.Zone
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Point
import ktx.math.plus

class ZoneWidget(val zone: Zone) : Widget()
{
	var tileSize = 32f
		set(value)
		{
			field = value
			renderer.tileSize = value
		}

	val border: Sprite = AssetManager.loadSprite("GUI/border", colour = Colour(Color(0.6f, 0.9f, 0.6f, 0.6f)))

	val TILE = 0
	val ENTITY = TILE+1
	val EFFECT = ENTITY+1

	val renderer = SortedRenderer(tileSize, zone.width.toFloat(), zone.height.toFloat(), EFFECT+1, true)

	val shapeRenderer = ShapeRenderer()

	val tempCol = Colour()

	var hpLossTimer = 0f

	var storedStatic = false

	init
	{
		touchable = Touchable.enabled

		addListener(object : InputListener()
					{
						override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
						{
							val xp = x + ((zone.width * tileSize) / 2f) - (width / 2f)

							val sx = (xp / tileSize).toInt()
							val sy = (y / tileSize).toInt()

							if (button == Input.Buttons.LEFT)
							{
								//zone.grid[sx, sy].isSolid = !zone.grid[sx, sy].isSolid

								val screenPos = pointToScreenspace(Vector2(sx.toFloat(), sy.toFloat()))
								screenPos.x += tileSize / 2f
								screenPos.y += tileSize / 2f
								val tile = if (zone.grid.inBounds(sx, sy)) zone.grid[sx, sy] else return false
							}

							//zone.select(Point(sx, sy))

							return true
						}

						override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
						{
							//zone.clearDrag()

							super.touchUp(event, x, y, pointer, button)
						}

						override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int)
						{
							val xp = x + ((zone.width * tileSize) / 2f) - (width / 2f)

							val sx = xp / tileSize
							val sy = y / tileSize
						}
					})
	}

	fun getRect(point: Point, actualSize: Boolean = false): Rectangle
	{
		val array = com.badlogic.gdx.utils.Array<Point>()
		array.add(point)
		val rect = getRect(array)

		if (actualSize)
		{
			rect.height *= 1.5f
		}

		return rect
	}

	fun getRect(points: com.badlogic.gdx.utils.Array<Point>): Rectangle
	{
		var minx = Float.MAX_VALUE
		var miny = Float.MAX_VALUE
		var maxx = -Float.MAX_VALUE
		var maxy = -Float.MAX_VALUE

		for (point in points)
		{
			val screenSpace = pointToScreenspace(point)
			if (screenSpace.x < minx)
			{
				minx = screenSpace.x
			}
			if (screenSpace.y < miny)
			{
				miny = screenSpace.y
			}
			if (screenSpace.x + tileSize > maxx)
			{
				maxx = screenSpace.x + tileSize
			}
			if (screenSpace.y + tileSize > maxy)
			{
				maxy = screenSpace.y + tileSize
			}
		}

		return Rectangle(minx, miny, maxx - minx, maxy - miny)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		val xp = x + (width / 2f) - ((zone.width * tileSize) / 2f)

		return Vector2(xp + point.x * tileSize, renderY + point.y * tileSize)
	}

	fun pointToScreenspace(point: Vector2): Vector2
	{
		val xp = x + (width / 2f) - ((zone.width * tileSize) / 2f)

		return Vector2(xp + point.x * tileSize, renderY + point.y * tileSize)
	}

	override fun invalidate()
	{
		super.invalidate()

		val w = width / zone.width.toFloat()
		val h = (height - 16f) / zone.height.toFloat()

		tileSize = Math.min(w, h)
	}

	var renderY = 0f
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		batch!!.end()

		val xp = this.x + (this.width / 2f) - ((zone.width * tileSize) / 2f)
		val yp = this.y
		renderY = yp

		if (!storedStatic)
		{
			storedStatic = true

			renderer.beginStatic()

			for (x in 0 until zone.width)
			{
				for (y in 0 until zone.height)
				{
					val tile = zone.grid[x, y]

					val tileColour = Colour.WHITE

					val xi = x.toFloat()
					val yi = y.toFloat()

					val groundSprite = tile.sprite
					if (!groundSprite.hasChosenSprites)
					{
						groundSprite.chooseSprites()
					}

					val sprite = groundSprite.chosenSprite
					if (sprite != null)
					{
						renderer.queueSprite(sprite, xi, yi, TILE, 0, tileColour)
					}

					val tilingSprite = groundSprite.chosenTilingSprite
					if (tilingSprite != null)
					{
						renderer.queueSprite(tilingSprite, xi, yi, TILE, 0, tileColour)
					}
				}
			}

			renderer.endStatic(batch)
		}

		renderer.begin(Gdx.app.graphics.deltaTime, xp, yp, Colour.WHITE)

		for (x in 0 until zone.width)
		{
			for (y in 0 until zone.height)
			{
				val tile = zone.grid[x, y]

				val tileColour = Colour.WHITE

				val xi = x.toFloat()
				val yi = y.toFloat()

				val groundSprite = tile.sprite
				if (!groundSprite.hasChosenSprites)
				{
					groundSprite.chooseSprites()
				}

				val sprite = groundSprite.chosenSprite
				if (sprite != null)
				{
					renderer.queueSprite(sprite, xi, yi, TILE, 0, tileColour)
				}

				val tilingSprite = groundSprite.chosenTilingSprite
				if (tilingSprite != null)
				{
					renderer.queueSprite(tilingSprite, xi, yi, TILE, 0, tileColour)
				}

				if (tile.isEncounter)
				{
					renderer.queueSprite(tile.flagSprite!!, xi, yi, TILE, 0, tileColour)
				}
			}
		}

		renderer.end(batch)

		batch.begin()

		shapeRenderer.projectionMatrix = batch.projectionMatrix
		shapeRenderer.transformMatrix = batch.transformMatrix
		shapeRenderer.updateMatrices()
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
		shapeRenderer.color = Color.PINK
		for (x in 0 until zone.width)
		{
			for (y in 0 until zone.height)
			{
				val tile = zone.grid[x, y]

				if (tile.isEncounter && tile.nextTile != null)
				{
					val n = tile.nextTile!!

					val p1 = pointToScreenspace(tile.toVec() + Vector2(0.5f, 0.5f))
					val p2 = pointToScreenspace(n.toVec() + Vector2(0.5f, 0.5f))

					//shapeRenderer.line(p1, p2)
				}
			}
		}
		shapeRenderer.end()
	}
}