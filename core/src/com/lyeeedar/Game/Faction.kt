package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Ascension
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.name
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Global
import com.lyeeedar.Rarity
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Systems.directionSprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.UI.tint
import com.lyeeedar.Util.*

class Faction(val path: String)
{
	lateinit var name: String
	lateinit var description: String
	lateinit var icon: Sprite

	val buffs = Array<Buff>(4) { Buff() }

	val heroes = com.badlogic.gdx.utils.Array<FactionEntity>(3)

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
			val hero = FactionEntity(this)
			hero.parse(el)

			heroes.add(hero)
		}
	}

	companion object
	{
		fun load(path: String): Faction
		{
			val path = "Factions/$path"
			val xml = getXml(path)
			val faction = Faction(path)
			faction.parse(xml)

			return faction
		}
	}
}

class FactionEntity(val faction: Faction)
{
	private lateinit var rawEntityPath: String
	lateinit var rarity: Rarity

	val entityPath: String
		get() = faction.path.directory() + "/" + rawEntityPath

	fun parse(xmlData: XmlData)
	{
		rawEntityPath = xmlData.get("Entity")
		rarity = Rarity.valueOf(xmlData.get("Rarity").toUpperCase())
	}

	fun createTile(size: Float, ascension: Ascension): Table
	{
		val entity = EntityLoader.load(entityPath)
		Global.engine.directionSprite().processEntity(entity, 0f)
		val sprite = Sprite((entity.renderable().renderable as Sprite).textures[0])

		val tileTable = Table()

		val background = AssetManager.loadSprite("Icons/Empty")
		val border = AssetManager.loadSprite("GUI/RewardChanceBorder")

		val tileStack = Stack()
		tileStack.add(SpriteWidget(background, size, size))
		tileStack.add(SpriteWidget(sprite, size, size))
		tileStack.add(SpriteWidget(border, size, size).tint(ascension.colour.color()))

		tileTable.add(tileStack).size(size)

		return tileTable
	}

	fun createCardTable(ascension: Ascension): Table
	{
		val entity = EntityLoader.load(entityPath)
		//entity.stats().ascension = ascension
		entity.stats().level = 1
		Global.engine.directionSprite().processEntity(entity, 0f)
		val sprite = Sprite((entity.renderable().renderable as Sprite).textures[0])

		val table = Table()

		val ascensionTable = Table()
		ascensionTable.add(SpriteWidget(faction.icon.copy(), 32f, 32f).addTapToolTip("Part of the ${faction.name} faction."))
		ascensionTable.add(
			SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = ascension.colour), 48f, 48f)
				.addTapToolTip("Ascension level " + (ascension.ordinal + 1)))
		ascensionTable.add(SpriteWidget(entity.stats().equipmentWeight.icon.copy(), 32f, 32f).addTapToolTip("Wears ${entity.stats().equipmentWeight.niceName} equipment."))
		ascensionTable.row()
		ascensionTable.add(Label(ascension.toString().neaten(), Global.skin).tint(ascension.colour.color())).colspan(3).center()

		table.add(ascensionTable).padBottom(5f)
		table.row()
		table.add(Label(entity.name().name, Global.skin, "cardtitle")).expandX().center().padTop(10f)
		table.row()
		table.add(Label(entity.name().title, Global.skin, "cardsmall").tint(Color.LIGHT_GRAY)).expandX().center().padTop(2f)
		table.row()
		table.add(SpriteWidget(sprite, 96f, 96f)).size(96f).expandX().center()
		table.row()

		val levelPowerTable = Table()
		fun addSubtextNumber(text: String, value: Int)
		{
			val table = Table()
			table.add(Label(value.prettyPrint(), Global.skin, "card")).expandX().center()
			table.row()
			val textLabel = Label(text, Global.skin, "cardsmall")
			textLabel.color = Color.GRAY
			table.add(textLabel).expandX().center()

			levelPowerTable.add(table).expandX()
		}
		addSubtextNumber("Ascension", ascension.ordinal+1)
		addSubtextNumber("Rating", entity.stats().calculatePowerRating(entity).toInt())

		table.add(levelPowerTable).growX().pad(3f)
		table.row()

		table.add(Table()).grow()
		table.row()

		return table
	}
}