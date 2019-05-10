package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.XmlData

class Symbol(var char: Char)
{
	var tileSprite: XmlData? = null
	var content: XmlData? = null

	fun write(data: Symbol, overwrite: Boolean = false)
	{
		if (overwrite)
		{
			tileSprite = null
			content = null
		}

		tileSprite = data.tileSprite ?: tileSprite
		content = data.content ?: content

		char = data.char
	}

	fun copy(): Symbol
	{
		val symbol = Symbol(char)

		symbol.write(this)

		return symbol
	}

	companion object
	{
		fun load(xmlData: XmlData, symbolTable: ObjectMap<Char, Symbol>) : Symbol
		{
			val char = xmlData.get("Character")[0]
			val extends = xmlData.get("Extends", "")?.firstOrNull()

			val symbol = if (extends != null) symbolTable[extends].copy() else Symbol(char)
			symbol.char = char
			symbol.tileSprite = xmlData.getChildByName("Tile") ?: symbol.tileSprite
			symbol.content = xmlData.getChildByName("Content") ?: symbol.content

			return symbol
		}
	}
}