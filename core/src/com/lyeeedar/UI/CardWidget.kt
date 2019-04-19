package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.ciel
import com.lyeeedar.Util.min
import ktx.actors.alpha
import ktx.actors.then
import ktx.collections.gdxArrayOf

data class Pick(val string: String, var pickFun: (card: CardWidget) -> Unit)

class CardWidget(val frontTable: Table, val frontDetailTable: Table, val backImage: TextureRegion, var data: Any? = null, val colour: Colour = Colour.WHITE, val border: Colour = Colour.TRANSPARENT) : WidgetGroup()
{
	val referenceWidth = Global.resolution.x - 100f
	val referenceHeight = Global.resolution.y - 200f

	val contentTable = Table()
	private val backTable: Table

	var canZoom = true
	var canPickFaceDown = false

	val pickFuns = Array<Pick>()
	var collapseFun: (() -> Unit)? = null
	var flipFun: (() -> Unit)? = null

	var flipEffect: ParticleEffectActor? = null
	var flipDelay = 0f

	var faceup = false
	private var flipping = false
	private var fullscreen = false
	var isPicked = false

	var clickable = true

	private val back = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 30, 30, 30, 30)
	private val backborder = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackgroundBorder"), 30, 30, 30, 30)

	init
	{
		addActor(contentTable)
		contentTable.background = NinePatchDrawable(back).tint(colour.color())

		backTable = Table()
		backTable.add(SpriteWidget(Sprite(backImage), referenceWidth - 60, referenceWidth - 60)).expand().center()

		contentTable.add(backTable).grow()

		contentTable.isTransform = true
		contentTable.originX = referenceWidth / 2
		contentTable.originY = referenceHeight / 2

		contentTable.setSize(referenceWidth, referenceHeight)

		addClickListener {
			if (clickable)
			{
				if (canZoom)
				{
					if (!fullscreen && faceup)
					{
						focus()
					}
					else if (!faceup)
					{
						flip(true)
					}
				}
				else
				{
					if (canPickFaceDown || faceup)
					{
						if (pickFuns.size > 0)
						{
							pickFuns[0].pickFun(this)
						}
					}
					else
					{
						flip(true)
					}
				}
			}
		}

		//debug()
	}

	fun addPick(string: String, pickFun: (card: CardWidget) -> Unit)
	{
		pickFuns.add(Pick(string, pickFun))
	}

	private data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
	private fun getTableBounds(): Bounds
	{
		val actualMidX = contentTable.x + contentTable.width / 2f
		val drawX = actualMidX - (contentTable.width * contentTable.scaleX) / 2f

		val actualMidY = contentTable.y + contentTable.height / 2f
		val drawY = actualMidY - (contentTable.height * contentTable.scaleY) / 2f

		return Bounds(x + drawX, y + drawY, contentTable.width * contentTable.scaleX, contentTable.height * contentTable.scaleY)
	}

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		val (tablex, tabley, width, height) = getTableBounds()
		val minx = tablex - this.x
		val miny = tabley - this.y

		if (touchable && this.touchable != Touchable.enabled) return null
		return if (x >= minx && x < minx+width && y >= miny && y < miny+height) this else null
	}

	override fun act(delta: Float)
	{
		super.act(delta)

		if (!fullscreen)
		{
			contentTable.act(delta)
		}
	}

	override fun setBounds(x: Float, y: Float, width: Float, height: Float)
	{
		super.setBounds(x, y, width, height)

		val scaleX = width / referenceWidth
		val scaleY = height / referenceHeight
		val min = min(scaleX, scaleY)

		contentTable.setScale(min)

		val middleX = width / 2f
		val middleY = height / 2f

		contentTable.setPosition(middleX - contentTable.width / 2f, middleY - contentTable.height / 2f)
	}

	fun getWidthFromHeight(height: Float): Float
	{
		val scaleY = height / referenceHeight
		return referenceWidth * scaleY
	}

	override fun positionChanged()
	{
		super.positionChanged()
		setBounds(x, y, width, height)
	}

	override fun rotationChanged()
	{
		super.rotationChanged()
		contentTable.rotation = rotation
	}

	override fun sizeChanged()
	{
		super.sizeChanged()
		setBounds(x, y, width, height)
	}

	override fun setPosition(x: Float, y: Float)
	{
		super.setPosition(x, y)

		setBounds(x, y, width, height)
	}

	override fun drawDebug(shapes: ShapeRenderer)
	{
		super.drawDebug(shapes)

		val bounds = getTableBounds()

		shapes.color = Color.OLIVE
		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.rect(x, y, width, height)

		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.color = Color.MAGENTA
		shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		if (!fullscreen)
		{
			if (!animating) setBounds(x, y, width, height)
			contentTable.draw(batch, parentAlpha * alpha)
		}
	}

	override fun drawChildren(batch: Batch?, parentAlpha: Float)
	{

	}

	fun setFacing(faceup: Boolean, animate: Boolean = true)
	{
		if (faceup != this.faceup)
		{
			flip(animate)
		}
	}

	private var animating = false
	fun flip(animate: Boolean = true)
	{
		if (flipping) return
		flipping = true

		if (animating) return

		val nextTable = if (faceup) backTable else frontTable
		val flipFun = fun () {
			this.flipFun?.invoke()

			contentTable.clearChildren()
			contentTable.add(nextTable).grow()
			flipping = false

			if (faceup)
			{
				contentTable.background = LayeredDrawable(NinePatchDrawable(back).tint(colour.color()), NinePatchDrawable(backborder).tint(border.color()))
			}
			else
			{
				contentTable.background = NinePatchDrawable(back).tint(colour.color())
			}
		}

		if (animate)
		{
			if (flipEffect != null)
			{
				flipEffect!!.setSize(width, height)
				flipEffect!!.setPosition(x, y)
				stage.addActor(flipEffect)
				toFront()
			}

			val scale = contentTable.scaleX
			val duration = 0.2f
			animating = true
			val sequence = delay(flipDelay) then scaleTo(0f, scale, duration) then lambda { flipFun() } then scaleTo(scale, scale, duration) then lambda{ animating = false }
			contentTable.addAction(sequence)
		}
		else
		{
			flipFun()
		}

		faceup = !faceup
	}

	fun focus()
	{
		if (fullscreen) { return }
		fullscreen = true

		val table = Table()
		val background = Table()

		val currentScale = contentTable.scaleX
		val point = contentTable.localToStageCoordinates(Vector2(0f, 0f))

		val zoomTable = Table()
		zoomTable.isTransform = true
		zoomTable.originX = referenceWidth / 2
		zoomTable.originY = referenceHeight / 2
		zoomTable.background = LayeredDrawable(NinePatchDrawable(back).tint(colour.color()), NinePatchDrawable(backborder).tint(border.color()))
		zoomTable.setPosition(contentTable.x, contentTable.y)
		zoomTable.setSize(referenceWidth, referenceHeight)
		zoomTable.setScale(contentTable.scaleX, contentTable.scaleY)

		// Setup anim
		val speed = 0.1f
		val destX = (stage.width - referenceWidth) / 2f
		val destY = (stage.height - referenceHeight) / 2f

		val collapseSequence = lambda {
			zoomTable.clear()

			val stack = Stack()
			stack.add(frontDetailTable)
			stack.add(frontTable)
			zoomTable.add(stack).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			frontDetailTable.addAction(hideSequence)
			frontTable.addAction(showSequence)
		} then parallel(scaleTo(currentScale, currentScale, speed), moveTo(point.x, point.y, speed)) then lambda {
			fullscreen = false

			background.remove()
			table.remove()
			table.clear()

			zoomTable.clear()
			contentTable.clear()
			contentTable.add(frontTable).grow()

			//contentTable.setScale(currentScale)
			//contentTable.setPosition(currentX, currentY)

			collapseFun?.invoke()
		}

		val expandSequence = lambda {
			contentTable.clear()

			val stack = Stack()
			stack.add(frontDetailTable)
			stack.add(frontTable)
			zoomTable.add(stack).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			frontTable.addAction(hideSequence)
			frontDetailTable.addAction(showSequence)
		} then parallel(scaleTo(1f, 1f, speed), moveTo(destX, destY, speed)) then lambda {
			zoomTable.clear()

			val stack = Stack()
			stack.add(frontDetailTable)

			val buttonTable = Table()
			stack.add(buttonTable)

			zoomTable.add(stack).grow()

			val closeButton = Button(Global.skin, "closecard")
			closeButton.addClickListener {
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}
			buttonTable.add(closeButton).expand().right().top().size(24f).pad(10f)
			buttonTable.row()

			val pickButtonTable = Table()

			for (pick in pickFuns)
			{
				val pickButton =  TextButton(pick.string, Global.skin, "defaultcard")
				pickButton.addClickListener {
					pick.pickFun(this)
					table.addAction(collapseSequence)
					background.addAction(fadeOut(speed))
				}

				pickButtonTable.add(pickButton).uniform()
			}

			buttonTable.add(pickButtonTable).expand().bottom().pad(10f)
		}

		// Create holder
		table.touchable = Touchable.enabled
		table.isTransform = true

		table.add(zoomTable).grow()
		zoomTable.setScale(1f)

		table.setSize(referenceWidth, referenceHeight)
		table.setPosition(point.x, point.y)
		table.setScale(currentScale)
		table.addAction(expandSequence)
		table.addClickListener {

		}

		// Background
		background.touchable = Touchable.enabled
		background.setFillParent(true)
		background.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		background.addClickListener {
			table.addAction(collapseSequence)
			background.addAction(fadeOut(speed))
		}
		background.alpha = 0f
		background.addAction(fadeIn(speed))

		Global.stage.addActor(background)
		Global.stage.addActor(table)
	}

	companion object
	{
		fun layoutCard(card: CardWidget, enterFrom: Direction, dstWidget: Table? = null, animate: Boolean = true)
		{
			layoutCards(gdxArrayOf(card), enterFrom, dstWidget, animate)
		}

		fun layoutCards(cardWidgets: Array<CardWidget>, enterFrom: Direction, dstWidget: Table? = null, animate: Boolean = true, flip: Boolean = false, startScale: Float = 1f)
		{
			val areapos = dstWidget?.localToStageCoordinates(Vector2()) ?: Vector2()
			val areaWidth = dstWidget?.width ?: Global.resolution.x.toFloat()
			val areaHeight = dstWidget?.height ?: Global.resolution.y.toFloat()

			var cardXCount = 1
			var cardYCount = (cardWidgets.size.toFloat() / cardXCount).ciel()

			val padding = 10f
			while (true)
			{
				val xSize = (areaWidth - (cardXCount+2)*padding) / cardXCount
				val ySize = (areaHeight - (cardYCount+2)*padding) / cardYCount

				val scaleX = xSize / cardWidgets[0].referenceWidth
				val scaleY = ySize / cardWidgets[0].referenceHeight
				val min = min(scaleX, scaleY)

				val cardWidth = cardWidgets[0].referenceWidth * min
				val cardHeight = cardWidgets[0].referenceHeight * min

				val testXCount = cardXCount+1
				val textYCount = (cardWidgets.size.toFloat() / testXCount).ciel()
				val testXSize = (areaWidth - (testXCount+2)*padding) / testXCount
				val testYSize = (areaHeight - (textYCount+2)*padding) / textYCount
				val testScaleX = testXSize / cardWidgets[0].referenceWidth
				val testScaleY = testYSize / cardWidgets[0].referenceHeight
				val testMin = min(testScaleX, testScaleY)

				val testCardWidth = cardWidgets[0].referenceWidth * testMin
				val testCardHeight = cardWidgets[0].referenceHeight * testMin

				if (testCardWidth > cardWidth || testCardHeight > cardHeight)
				{
					cardXCount = testXCount
					cardYCount = textYCount
				}
				else
				{
					break
				}
			}

			// Calculate card sizes
			val xSize = min((areaWidth - padding*2)/2f, (areaWidth - (cardXCount+2)*padding) / cardXCount)
			val ySize = min((areaHeight - padding*2)/2f, (areaHeight - (cardYCount+2)*padding) / cardYCount)

			val normalLayout = (cardWidgets.size.toFloat() / cardXCount).toInt() * cardXCount
			val finalRowLen = cardWidgets.size - normalLayout
			val finalRowStep = (areaWidth - (finalRowLen+2)*padding) / finalRowLen

			var x = 0
			var y = 0
			for (i in 0 until cardWidgets.size)
			{
				val widget = cardWidgets[i]

				if (i >= normalLayout)
				{
					widget.setPosition(areapos.x + padding + finalRowStep*0.5f + x * finalRowStep + x * padding - xSize*0.5f, areapos.y + padding + y * ySize + y * padding)
				}
				else
				{
					widget.setPosition(areapos.x + padding + x * xSize + x * padding, areapos.y + padding + y * ySize + y * padding)
				}

				x++
				if (x == cardXCount)
				{
					x = 0
					y++
				}
			}

			for (widget in cardWidgets)
			{
				widget.setSize(xSize, ySize)
			}

			// do animation
			if (animate)
			{
				val startXSize = xSize * startScale
				val startYSize = ySize * startScale

				// calculate start position
				val startX: Float = when (enterFrom)
				{
					Direction.CENTER -> areaWidth / 2f - startXSize / 2f
					Direction.NORTH -> areaWidth / 2f - startXSize / 2f
					Direction.SOUTH -> areaWidth / 2f - startXSize / 2f

					Direction.EAST -> areaWidth
					Direction.NORTHEAST -> areaWidth
					Direction.SOUTHEAST -> areaWidth

					Direction.WEST -> -startXSize
					Direction.NORTHWEST -> -startXSize
					Direction.SOUTHWEST -> -startXSize
				}

				val startY: Float = when (enterFrom)
				{
					Direction.CENTER -> areaHeight / 2f - startYSize / 2f
					Direction.EAST -> areaHeight / 2f - startYSize / 2f
					Direction.WEST -> areaHeight / 2f - startYSize / 2f

					Direction.NORTH -> areaHeight
					Direction.NORTHWEST -> areaHeight
					Direction.NORTHEAST -> areaHeight

					Direction.SOUTH -> -startYSize
					Direction.SOUTHWEST -> -startYSize
					Direction.SOUTHEAST -> -startYSize
				}

				for (widget in cardWidgets)
				{
					widget.setSize(startXSize, startYSize)
				}

				var delay = 0.2f
				for (widget in cardWidgets)
				{
					val x = widget.x
					val y = widget.y

					widget.setPosition(startX + areapos.x, startY + areapos.y)

					widget.clickable = false

					val delayVal = delay
					delay += 0.04f
					val sequence = delay(delayVal) then parallel(moveTo(x, y, 0.2f), sizeTo(xSize, ySize, 0.2f)) then delay(0.1f) then lambda {

						if (flip) widget.setFacing(true, true)
						widget.clickable = true
					}

					widget.addAction(sequence)
				}
			}
		}
	}
}