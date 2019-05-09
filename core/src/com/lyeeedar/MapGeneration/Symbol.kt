package com.lyeeedar.MapGeneration

import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class Symbol(var char: Char)
{
	val contents = FastEnumMap<SpaceSlot, XmlData>(SpaceSlot::class.java)

	fun write(data: FastEnumMap<SpaceSlot, XmlData>)
	{
		for (slot in SpaceSlot.Values)
		{
			if (data.containsKey(slot))
			{
				contents[slot] = data[slot]
			}
		}
	}

	fun write(data: Symbol, overwrite: Boolean = false)
	{
		if (overwrite)
		{
			contents.clear()
		}

		write(data.contents)
		char = data.char
	}

	fun copy(): Symbol
	{
		val symbol = Symbol(char)

		symbol.write(contents)

		return symbol
	}
}