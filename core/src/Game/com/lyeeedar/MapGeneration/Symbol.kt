package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.XmlData

class Symbol(var char: Char) : IPathfindingTile
{
	enum class SymbolType
	{
		FLOOR,
		WALL,
		PIT
	}

	var tileSprite: XmlData? = null
	var content: XmlData? = null

	var type: SymbolType = SymbolType.FLOOR

	var locked = false

	var placerHashCode: Int = -1

	fun write(data: Symbol, overwrite: Boolean = false)
	{
		if (locked) return

		if (overwrite)
		{
			tileSprite = null
			content = null
		}

		tileSprite = data.tileSprite ?: tileSprite
		content = data.content ?: content

		char = data.char
		type = data.type

		locked = data.locked
	}

	fun copy(): Symbol
	{
		val symbol = Symbol(char)

		symbol.write(this)

		return symbol
	}

	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		if (locked && type != SymbolType.FLOOR) return false
		return true
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?): Int
	{
		var influence = 0
		if (type != SymbolType.FLOOR) influence += 200
		if (content != null) influence += 50
		if (char != '.') influence += 50

		return influence
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

			symbol.type = SymbolType.valueOf(xmlData.get("Type", symbol.type.toString())!!.toUpperCase())

			symbol.locked = xmlData.getBoolean("Locked", false)

			return symbol
		}
	}
}