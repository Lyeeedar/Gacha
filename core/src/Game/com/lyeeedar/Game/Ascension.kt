package com.lyeeedar.Game

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random
import squidpony.squidmath.LightRNG

// ----------------------------------------------------------------------
enum class Ascension(val multiplier: Float, val colour: Colour, val shardsRequired: Int)
{
	MUNDANE(1f, Colour(152, 127, 95, 255, true), 0),
	EXCEPTIONAL(1.25f, Colour(78, 232, 67, 255, true), 6),
	EXTRAORDINARY(1.6f, Colour(72, 169, 255, 255, true), 12),
	FABLED(1.9f, Colour(186, 85, 223, 255, true), 24),
	LEGENDARY(2.4f, Colour(255, 219, 0, 255, true), 48),
	MYTHICAL(2.8f, Colour(226, 0, 51, 255, true), 96),
	DIVINE(3.5f, Colour(255, 221, 249, 255, true), 200);

	val nextAscension: Ascension
		get()
		{
			if (ordinal < Values.size-1)
			{
				return Values[ordinal+1]
			}

			return this
		}

	companion object
	{
		val Values = Ascension.values()

		fun get(index: Int): Ascension
		{
			if (index >= Values.size)
			{
				return Values.last()
			}
			else
			{
				return Values[index]
			}
		}

		fun getWeightedAscension(ran: LightRNG = Random.random): Ascension
		{
			val validAscensions = Array<Ascension>(2000)
			var weightCounter = 1000
			for (i in 0 until 4)
			{
				val ascension = Ascension.Values[i]
				for (ii in 0 until weightCounter)
				{
					validAscensions.add(ascension)
				}

				weightCounter /= 4
			}
			val ascension = validAscensions.random(ran)
			return ascension
		}
	}
}