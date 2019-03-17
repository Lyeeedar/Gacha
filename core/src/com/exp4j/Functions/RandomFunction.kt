package com.exp4j.Functions

import com.badlogic.gdx.utils.Pool

class RandomFunction : SeededFunction("rnd", 1)
{
	override fun apply(vararg arg0: Double): Double
	{
		return ran.nextFloat() * arg0[0]
	}

	override fun free()
	{
		synchronized(pool)
		{
			pool.free(this)
		}
	}

	companion object
	{

		val pool = object : Pool<RandomFunction>() {
			override fun newObject(): RandomFunction
			{
				return RandomFunction()
			}
		}

		fun obtain(): RandomFunction
		{
			synchronized(pool)
			{
				return pool.obtain()
			}
		}
	}
}
