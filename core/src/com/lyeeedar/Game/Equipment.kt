package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.EventAndCondition
import com.lyeeedar.Components.applyAscensionAndLevel
import com.lyeeedar.Renderables.Sprite.MaskedTextureData
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import kotlin.coroutines.experimental.buildSequence

interface IEquipmentStatsProvider
{
	var name: String
	var description: String

	val stats: FastEnumMap<Statistic, Float>
	val eventHandlers: FastEnumMap<EventType, Array<EventAndCondition>>
}

class Equipment : IEquipmentStatsProvider
{
	override lateinit var name: String
	override lateinit var description: String

	val fullName: String
		get() = statsProviders.map { it.name }.joinToString(" ")

	val fullDescription: String
		get() = statsProviders.map { it.description }.filter { it.isNotEmpty() }.joinToString { "\n" }

	lateinit var icon: MaskedTextureData

	val fullIcon: MaskedTextureData
		get() {
			val data = MaskedTextureData()
			data.base = icon.base
			data.mask = icon.mask
			data.layer1 = material?.layerTexture ?: icon.layer1
			data.layer2 = prefix?.layerTexture ?: icon.layer2
			data.layer3 = suffix?.layerTexture ?: icon.layer3

			return data
		}

	var weight: EquipmentWeight = EquipmentWeight.MEDIUM
	var slot: EquipmentSlot = EquipmentSlot.WEAPON

	var ascension: Ascension = Ascension.MUNDANE
	var level: Int = 1

	override val stats = FastEnumMap<Statistic, Float>(Statistic::class.java)
	override val eventHandlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	var prefix: Part? = null
	var material: Part? = null
	var suffix: Part? = null

	val statsProviders: Sequence<IEquipmentStatsProvider>
		get(){
			return buildSequence {
				if (prefix != null) yield(prefix!!)
				if (material != null) yield(material!!)
				yield(this@Equipment)
				if (suffix != null) yield(suffix!!)
			}
		}

	fun getStat(statistic: Statistic): Float
	{
		var value = 0f
		for (provider in statsProviders)
		{
			value += provider.stats[statistic] ?: 0f
		}

		// apply level / rarity, but only to the base stats
		if (Statistic.BaseValues.contains(statistic))
		{
			value = value.applyAscensionAndLevel(level, ascension)
		}

		return value
	}

	fun calculatePowerRating(): Float
	{
		var hp = getStat(Statistic.MAXHP)
		hp *= 1f + getStat(Statistic.AEGIS)
		hp *= 1f + getStat(Statistic.DR)
		hp *= 1f + getStat(Statistic.REGENERATION) * 2f
		hp *= 1f + getStat(Statistic.LIFESTEAL)

		var power = getStat(Statistic.POWER) * 10f
		power += (power * (1f + getStat(Statistic.CRITDAMAGE))) * getStat(Statistic.CRITCHANCE)
		power *= 1f + getStat(Statistic.HASTE)

		var rating = hp + power

		for (provider in statsProviders)
		{
			for (handlers in provider.eventHandlers)
			{
				rating *= 1f + 0.2f * handlers.size
			}
		}

		return rating
	}

	fun createCardTable(): Table
	{
		val table = Table()

		val ascensionTable = Table()
		val slotStack = Stack()
		slotStack.add(SpriteWidget(AssetManager.loadSprite("Icons/icon_base"), 32f, 32f))
		slotStack.add(SpriteWidget(slot.icon.copy(), 32f, 32f))
		slotStack.addTapToolTip("${slot.name.neaten()} slot.")
		ascensionTable.add(slotStack)
		ascensionTable.add(SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = ascension.colour), 48f, 48f))
		ascensionTable.add(SpriteWidget(weight.icon.copy(), 32f, 32f).addTapToolTip("${weight.niceName} equipment."))
		ascensionTable.row()
		ascensionTable.add(Label(ascension.toString().neaten(), Global.skin).tint(ascension.colour.color())).colspan(3).center()

		table.add(ascensionTable).padBottom(10f)
		table.row()
		table.add(Label(fullName, Global.skin, "cardtitle").wrap().align(Align.center)).growX().center()
		table.row()
		table.add(MaskedTexture(fullIcon)).size(64f).expandX().center()
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
		addSubtextNumber("Level", level)
		addSubtextNumber("Rating", calculatePowerRating().toInt())

		table.add(levelPowerTable).growX().pad(5f)
		table.row()

		table.add(Label(fullDescription, Global.skin, "card").wrap()).growX().center()
		table.row()

		var bright = true
		for (stat in Statistic.Values)
		{
			val statTable = Table()

			val rawValue = getStat(stat)
			val value: Int
			val valueStr: String
			if (Statistic.BaseValues.contains(stat))
			{
				value = rawValue.toInt()
				valueStr = value.prettyPrint()
			}
			else
			{
				value = (rawValue * 100f).toInt()
				valueStr = value.prettyPrint() + "%"
			}

			if (value != 0)
			{
				val rowTable = Table()
				rowTable.add(Label(stat.niceName, Global.skin, "cardsmall")).pad(7f)
				rowTable.add(Label(valueStr, Global.skin, "cardsmall")).expandX().right().pad(7f)
				rowTable.addTapToolTip(stat.niceName + ":\n\n" + stat.tooltip)

				if (bright)
				{
					rowTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
				}

				table.add(rowTable).growX()
				table.row()

				bright = !bright
			}

			table.add(statTable).growX()
			table.row()
		}

		table.add(Table()).grow()
		table.row()

		return table
	}

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name", "")!!
		description = xmlData.get("Description", "")!!

		icon = MaskedTextureData(xmlData.getChildByName("Icon")!!)

		weight = EquipmentWeight.valueOf(xmlData.get("Weight", "Medium")!!.toUpperCase())
		slot = EquipmentSlot.valueOf(xmlData.get("Slot", "Weapon")!!.toUpperCase())

		level = xmlData.getInt("Level", 1)
		ascension = Ascension.valueOf(xmlData.get("Ascension", "Mundane")!!.toUpperCase())

		Statistic.parse(xmlData.getChildByName("Statistics"), stats)
		EventType.parseEvents(xmlData.getChildByName("EventHandlers"), eventHandlers)
	}

	companion object
	{
		fun load(xmlData: XmlData): Equipment
		{
			val equip = Equipment()
			equip.parse(xmlData)

			return equip
		}
	}
}