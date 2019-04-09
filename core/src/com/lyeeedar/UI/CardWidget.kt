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
import com.lyeeedar.Util.min
import ktx.actors.alpha
import ktx.actors.then

data class Pick(val string: String, var pickFun: (card: CardWidget) -> Unit)

class CardWidget(val frontTable: Table, val frontDetailTable: Table, val backImage: TextureRegion, var data: Any?, val colour: Colour = Colour.WHITE) : WidgetGroup()
{
	val referenceWidth = Global.resolution.x - 100f
	val referenceHeight = Global.resolution.y - 200f

	val contentTable = Table()
	val backTable: Table

	var canZoom = true
	var canPickFaceDown = false

	val pickFuns = Array<Pick>()
	var collapseFun: (() -> Unit)? = null

	private var faceup = false
	private var flipping = false
	private var fullscreen = false

	var clickable = true

	val back = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 30, 30, 30, 30)

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

	data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
	fun getTableBounds(): Bounds
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

	var animating = false
	fun flip(animate: Boolean = true)
	{
		if (flipping) return
		flipping = true

		if (animating) return

		val nextTable = if (faceup) backTable else frontTable
		val flipFun = fun () { contentTable.clearChildren(); contentTable.add(nextTable).grow(); flipping = false }

		if (animate)
		{
			val scale = contentTable.scaleX
			val speed = 0.2f
			animating = true
			val sequence = scaleTo(0f, scale, speed) then lambda { flipFun() } then scaleTo(scale, scale, speed) then lambda{ animating = false }
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
		zoomTable.background = NinePatchDrawable(back).tint(colour.color())
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
		fun layoutCards(cardWidgets: Array<CardWidget>, enterFrom: Direction, dstWidget: Table? = null, animate: Boolean = true)
		{
			val areapos = dstWidget?.localToStageCoordinates(Vector2()) ?: Vector2()
			val areaWidth = dstWidget?.width ?: Global.resolution.x.toFloat()
			val areaHeight = dstWidget?.height ?: Global.resolution.y.toFloat()

			// Calculate card sizes
			val padding = 20f
			val cardWidth = (areaWidth - 3f * padding) / 2f
			val cardHeight = (areaHeight - 3f * padding) / 2f

			// Calculate final positions
			if (cardWidgets.size == 1)
			{
				// center
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, areaHeight / 2f - cardHeight / 2f + areapos.y)
			}
			else if (cardWidgets.size == 2)
			{
				// vertical alignment
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding * 2f + cardHeight + areapos.y)
				cardWidgets[1].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding + areapos.y)
			}
			else if (cardWidgets.size == 3)
			{
				// triangle, single card at top, 2 below
				cardWidgets[0].setPosition(areaWidth / 2f - cardWidth / 2f + areapos.x, padding * 2f + cardHeight + areapos.y)
				cardWidgets[1].setPosition(padding + areapos.x, padding + areapos.y)
				cardWidgets[2].setPosition(padding * 2f + cardWidth + areapos.x, padding + areapos.y)
			}
			else if (cardWidgets.size == 4)
			{
				// even grid
				cardWidgets[0].setPosition(padding + areapos.x, padding * 2f + cardHeight + areapos.y)
				cardWidgets[1].setPosition(padding + areapos.x, padding + areapos.y)
				cardWidgets[2].setPosition(padding * 2f + cardWidth + areapos.x, padding + areapos.y)
				cardWidgets[3].setPosition(padding * 2f + cardWidth + areapos.x, padding * 2f + cardHeight + areapos.y)
			}

			for (widget in cardWidgets)
			{
				widget.setSize(cardWidth, cardHeight)
			}

			// do animation
			if (animate)
			{
				// calculate start position
				val startX: Float = when (enterFrom)
				{
					Direction.CENTER -> areaWidth / 2f - cardWidth / 2f
					Direction.NORTH -> areaWidth / 2f - cardWidth / 2f
					Direction.SOUTH -> areaWidth / 2f - cardWidth / 2f

					Direction.EAST -> areaWidth
					Direction.NORTHEAST -> areaWidth
					Direction.SOUTHEAST -> areaWidth

					Direction.WEST -> -cardWidth
					Direction.NORTHWEST -> -cardWidth
					Direction.SOUTHWEST -> -cardWidth
				}

				val startY: Float = when (enterFrom)
				{
					Direction.CENTER -> areaHeight / 2f - cardHeight / 2f
					Direction.EAST -> areaHeight / 2f - cardHeight / 2f
					Direction.WEST -> areaHeight / 2f - cardHeight / 2f

					Direction.NORTH -> areaHeight
					Direction.NORTHWEST -> areaHeight
					Direction.NORTHEAST -> areaHeight

					Direction.SOUTH -> -cardHeight
					Direction.SOUTHWEST -> -cardHeight
					Direction.SOUTHEAST -> -cardHeight
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
					val sequence = delay(delayVal) then moveTo(x, y, 0.2f) then delay(0.1f) then lambda { widget.setFacing(true, true); widget.clickable = true }
					widget.addAction(sequence)
				}
			}
		}
	}
}