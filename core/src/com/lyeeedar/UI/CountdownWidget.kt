package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Global
import com.lyeeedar.Util.pluralize
import java.lang.System.currentTimeMillis
import kotlin.math.max

class CountdownWidget(val caption: String, val targetTime: Long) : Table()
{
	val captionLabel: Label = Label(caption, Global.skin, "small")
	val hoursLabel: Label = Label("0 hours", Global.skin, "small").align(Align.right)
	val minutesLabel: Label = Label("00 minutes", Global.skin, "small").align(Align.right)
	val secondsLabel: Label = Label("00 seconds", Global.skin, "small").align(Align.right)

	init
	{
		add(captionLabel).pad(2f)
		add(hoursLabel).width(hoursLabel.prefWidth).pad(2f)
		add(minutesLabel).width(minutesLabel.prefWidth).pad(2f)
		add(secondsLabel).width(secondsLabel.width).pad(2f)

		act(0f)
	}

	override fun act(delta: Float)
	{
		val currentTime = currentTimeMillis()
		val difference = targetTime - currentTime
		val seconds = difference / 1000L
		val minutes = seconds / 60L
		val hours = minutes / 60L

		val displayHours = max(hours, 0)
		val displayMinutes = max(minutes - hours * 60L, 0)
		val displaySeconds = max(seconds - minutes * 60L, 0)

		hoursLabel.setText("$displayHours" + " hour".pluralize(displayHours.toInt()))
		minutesLabel.setText("$displayMinutes" + " minute".pluralize(displayMinutes.toInt()))
		secondsLabel.setText("$displaySeconds" + " second".pluralize(displaySeconds.toInt()))

		super.act(delta)
	}
}