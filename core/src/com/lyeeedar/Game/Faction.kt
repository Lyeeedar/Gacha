package com.lyeeedar.Game

import com.lyeeedar.Components.Buff
import com.lyeeedar.Rarity
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class Faction
{
	lateinit var name: String
	lateinit var description: String
	lateinit var icon: Sprite

	val buffs = Array<Buff>(4) { Buff() }

	val heroes = com.badlogic.gdx.utils.Array<FactionEntity>()

	var factionXP = 0

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name")
		description = xmlData.get("Description")
		icon = AssetManager.loadSprite(xmlData.getChildByName("Icon")!!)

		val buffsEl = xmlData.getChildByName("Buffs")!!
		buffs[0] = Buff.load(buffsEl.getChildByName("Buff2")!!)
		buffs[1] = Buff.load(buffsEl.getChildByName("Buff3")!!)
		buffs[2] = Buff.load(buffsEl.getChildByName("Buff4")!!)
		buffs[3] = Buff.load(buffsEl.getChildByName("Buff5")!!)

		for (buff in buffs)
		{
			buff.duration = 9999
		}

		val heroesEl = xmlData.getChildByName("Heroes")!!
		for (el in heroesEl.children)
		{
			val hero = FactionEntity()
			hero.parse(el)
			hero.faction = this

			heroes.add(hero)
		}
	}

	companion object
	{
		fun load(path: String): Faction
		{
			val xml = getXml("Factions/$path")
			val faction = Faction()
			faction.parse(xml)

			return faction
		}
	}
}

class FactionEntity
{
	lateinit var entityPath: String
	lateinit var rarity: Rarity
	lateinit var faction: Faction

	fun parse(xmlData: XmlData)
	{
		entityPath = xmlData.get("Entity")
		rarity = Rarity.valueOf(xmlData.get("Rarity").toUpperCase())
	}
}