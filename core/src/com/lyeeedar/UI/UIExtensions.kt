package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import ktx.actors.alpha
import ktx.actors.plus
import ktx.actors.then
import ktx.scene2d.label
import ktx.scene2d.table

fun showFullscreenText(text: String, minDuration: Float, exitAction: ()->Unit)
{
	val fadeTable = table {
		label(text, "default", Global.skin) {
			cell -> cell.top()
		}
	}

	fadeTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/white.png")).tint(Color(0f, 0f, 0f, 0.7f))
	fadeTable.alpha = 0f

	val sequence = Actions.alpha(0f) then Actions.fadeIn(0.2f) then lambda {
		val outsequence = Actions.fadeOut(0.2f) then Actions.removeActor()

		Future.call({
			Global.controls.onInput += fun(key): Boolean {
				fadeTable + outsequence
				exitAction.invoke()
				return true
			}
		}, minDuration)
	}

	fadeTable + sequence

	Global.stage.addActor(fadeTable)
	fadeTable.setFillParent(true)
}

fun Batch.setColor(col: Colour)
{
	this.setColor(col.toFloatBits())
}

fun Actor.tint(col: Color): Actor
{
	this.color = col
	return this
}

fun Label.wrap(): Label
{
	this.setWrap(true)
	return this
}

fun Label.align(alignment: Int): Label
{
	this.setAlignment(alignment)
	return this
}

fun <T : Actor> Stack.addTable(actor: T): Cell<T>
{
	val holderTable = Table()
	val cell = holderTable.add(actor)
	this.add(holderTable)
	return cell
}