package com.lyeeedar.UI

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Path
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.getRotation

class ShakeAction(val amount: Float, val speed: Float, val duration: Float) : Action()
{
	var time = 0f

	var shakeRadius = amount
	var shakeAccumulator = 0f
	var shakeAngle = 0f

	var offsetx = 0f
	var offsety = 0f

	override fun act(delta: Float): Boolean
	{
		time += delta
		shakeAccumulator += delta
		while ( shakeAccumulator >= speed )
		{
			shakeAccumulator -= speed
			shakeAngle += (150 + Random.random() * 60)
		}

		target.moveBy(-offsetx, -offsety)

		offsetx = Math.sin( shakeAngle.toDouble() ).toFloat() * shakeRadius
		offsety = Math.cos( shakeAngle.toDouble() ).toFloat() * shakeRadius

		target.moveBy(offsetx, offsety)

		return time >= duration
	}
}

fun shake(amount: Float, speed: Float, duration: Float): ShakeAction = ShakeAction(amount, speed, duration)

class LambdaAction(val lambda: ()->Unit) : Action()
{
	override fun act(delta: Float): Boolean
	{
		lambda.invoke()

		return true
	}
}

fun lambda(lambda: ()->Unit): LambdaAction = LambdaAction(lambda)

class MoteAction(val path: Path<Vector2>, val duration: Float, val interpolation: Interpolation) : Action()
{
	var time: Float = 0f

	val newPos = Vector2()
	override fun act(delta: Float): Boolean
	{
		time += delta

		val currentPos = Vector2(target.x, target.y)

		val alpha = time / duration
		val interpAlpha = interpolation.apply(alpha)
		path.valueAt(newPos, interpAlpha)

		target.setPosition(newPos.x, newPos.y)
		target.rotation = getRotation(currentPos, newPos)

		return time >= duration
	}
}

fun mote(path: Path<Vector2>, duration: Float, interpolation: Interpolation = Interpolation.linear): MoteAction = MoteAction(path, duration, interpolation)