package com.exp4j.Functions

import squidpony.squidmath.LightRNG

abstract class SeededFunction(name: String, numArguments: Int) : net.objecthunter.exp4j.function.Function(name, numArguments)
{
	protected val ran = LightRNG()

	fun set(seed: Long)
	{
		this.ran.setSeed(seed)
	}

	abstract fun free()
}