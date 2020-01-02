package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.pluralize
import java.lang.System.currentTimeMillis
import kotlin.math.max

class CountdownWidget(val caption: String, val targetTime: Long, val short: Boolean = false) : Table()
{
	val captionLabel: Label = Label(caption, Statics.skin, "small")
	val hoursLabel: Label
	val minutesLabel: Label
	val secondsLabel: Label

	init
	{
		if (short)
		{
			hoursLabel = Label("00 :", Statics.skin, "small").align(Align.right)
			minutesLabel = Label("00 :", Statics.skin, "small").align(Align.right)
			secondsLabel = Label("00 ", Statics.skin, "small").align(Align.right)
		}
		else
		{
			hoursLabel = Label("0 hours", Statics.skin, "small").align(Align.right)
			minutesLabel = Label("00 minutes", Statics.skin, "small").align(Align.right)
			secondsLabel = Label("00 seconds", Statics.skin, "small").align(Align.right)
		}

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

		if (short)
		{
			hoursLabel.setText("$displayHours : ")
			minutesLabel.setText("$displayMinutes : ")
			secondsLabel.setText("$displaySeconds")
		}
		else
		{
			hoursLabel.setText("$displayHours" + " hour".pluralize(displayHours.toInt()))
			minutesLabel.setText("$displayMinutes" + " minute".pluralize(displayMinutes.toInt()))
			secondsLabel.setText("$displaySeconds" + " second".pluralize(displaySeconds.toInt()))
		}

		super.act(delta)
	}
}