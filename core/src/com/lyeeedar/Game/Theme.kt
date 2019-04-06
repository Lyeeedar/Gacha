package com.lyeeedar.Game

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Theme(val filepath: String)
{
	val symbols = Array<Symbol>()

	lateinit var backgroundTile: TextureRegion

	var ambient = Colour()

	companion object
	{
		fun load(path: String): Theme
		{
			val xml = getXml("Themes/$path")
			val theme = Theme(path)

			theme.backgroundTile = AssetManager.loadTextureRegion(xml.get("BackgroundTile"))!!

			val symbolsEl = xml.getChildByName("Symbols")!!
			for (symbolEl in symbolsEl.children)
			{
				val symbol = Symbol.parse(symbolEl)
				theme.symbols.add(symbol)
			}

			theme.ambient = AssetManager.loadColour(xml.getChildByName("Ambient")!!)

			return theme
		}
	}
}

class Symbol(
	val char: Char, val extends: Char,
	val usageCondition: String, val fallbackChar: Char,
	val nameKey: String?,
	val sprite: SpriteWrapper?)
{
	companion object
	{
		fun parse(xmlData: XmlData): Symbol
		{
			val character = xmlData.get("Character")[0]
			val extends = xmlData.get("Extends", " ")!!.firstOrNull() ?: ' '

			val usageCondition = xmlData.get("UsageCondition", "1")!!.toLowerCase()
			val fallbackChar = xmlData.get("FallbackCharacter", ".")!!.firstOrNull() ?: '.'

			val nameKey = xmlData.get("NameKey", null)

			var sprite: SpriteWrapper? = null
			val symbolSpriteEl = xmlData.getChildByName("Sprite")
			if (symbolSpriteEl != null)
			{
				sprite = SpriteWrapper.load(symbolSpriteEl)
			}

			return Symbol(character, extends, usageCondition, fallbackChar, nameKey, sprite)
		}
	}
}