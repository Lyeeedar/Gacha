package com.lyeeedar.Game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.*
import com.lyeeedar.Game.ActionSequence.BuffAction
import com.lyeeedar.Game.ActionSequence.DamageAction
import com.lyeeedar.Game.ActionSequence.HealAction
import com.lyeeedar.Renderables.Sprite.MaskedTextureData
import com.lyeeedar.Renderables.Sprite.Sprite
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
			data.glow = icon.glow
			data.mask = icon.mask
			data.layer1 = material?.layerTexture ?: icon.layer1
			data.layer2 = prefix?.layerTexture ?: icon.layer2
			data.layer3 = suffix?.layerTexture ?: icon.layer3

			return data
		}

	var weight: EquipmentWeight = EquipmentWeight.MEDIUM
	var slot: EquipmentSlot = EquipmentSlot.WEAPON

	var ascension: Ascension = Ascension.MUNDANE

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

	fun getStat(statistic: Statistic, level: Int): Float
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
		else
		{
			// lerp rest by ascension

			val alpha = ascension.multiplier / Ascension.Values.last().multiplier
			value = 0f.lerp(value, alpha)

			value *= 1f + level / 1000f
		}

		return value
	}

	fun calculatePowerRating(entity: Entity? = null): Float
	{
		val stats = entity?.stats() ?: StatisticsComponent.createSample()

		var hp = stats.baseStats[Statistic.MAXHP].applyAscensionAndLevel(stats.level, stats.ascension) + getStat(Statistic.MAXHP, stats.level)
		hp = max(hp, 1f)

		hp *= 1f + getStat(Statistic.AEGIS, stats.level)
		hp *= 1f + getStat(Statistic.DR, stats.level)
		hp *= 1f + getStat(Statistic.REGENERATION, stats.level) * 2f

		var power = stats.baseStats[Statistic.POWER].applyAscensionAndLevel(stats.level, stats.ascension) * 10f + getStat(Statistic.POWER, stats.level) * 10f
		power = max(power, 1f)

		val critDam = 1f + getStat(Statistic.CRITDAMAGE, stats.level) + stats.baseStats[Statistic.CRITDAMAGE]
		val critChance = getStat(Statistic.CRITCHANCE, stats.level) + stats.baseStats[Statistic.CRITCHANCE]
		power += (power * critDam) * critChance
		power *= 1f + getStat(Statistic.HASTE, stats.level)
		power *= 1f + getStat(Statistic.LIFESTEAL, stats.level)

		var rating = hp + power

		for (provider in statsProviders)
		{
			for (handlers in provider.eventHandlers)
			{
				rating *= 1f + 0.2f * handlers.size
			}
		}

		val ability = entity?.ability()
		if (ability != null)
		{
			var abilityModifier = 0f

			for (ability in ability.abilities)
			{
				if (ability.ability.actions.any { it is DamageAction || it is HealAction })
				{
					abilityModifier += getStat(Statistic.ABILITYPOWER, stats.level)
				}

				if (ability.ability.actions.any { it is BuffAction && !it.isDebuff })
				{
					abilityModifier += getStat(Statistic.BUFFPOWER, stats.level)
				}

				if (ability.ability.actions.any { it is BuffAction && it.isDebuff })
				{
					abilityModifier += getStat(Statistic.DEBUFFPOWER, stats.level)
				}

				abilityModifier += getStat(Statistic.ABILITYCOOLDOWN, stats.level)
			}

			rating *= 1f + abilityModifier * 0.5f
		}

		return rating
	}

	fun createTile(size: Float): Table
	{
		val equipmentStack = Stack()
		if (ascension == Ascension.DIVINE || ascension == Ascension.LEGENDARY)
		{
			val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back_bright"), size, size)
			tileBack.color = ascension.colour.color()
			equipmentStack.add(tileBack)
		}
		else
		{
			val tileBack = SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), size, size)
			tileBack.color = ascension.colour.color()
			equipmentStack.add(tileBack)
		}

		equipmentStack.add(
			SpriteWidget(AssetManager.loadSprite("GUI/background_stars"), size, size)
				.tint(Color(1f, 1f, 1f, ascension.ordinal.toFloat() / Ascension.Values.size.toFloat())))
		equipmentStack.add(SpriteWidget(Sprite(fullIcon.glow), size, size).tint(ascension.colour.color().lerp(Color.WHITE, 0.6f)))

		val tileFront = MaskedTexture(fullIcon)
		equipmentStack.add(tileFront)
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), size, size))
		equipmentStack.add(SpriteWidget(AssetManager.loadSprite("GUI/EquipmentBorder"), size, size).tint(ascension.colour.color()))

		val table = Table()
		table.add(equipmentStack).grow()
		return table
	}

	fun createCardTable(entity: Entity? = null): Table
	{
		val table = Table()

		val ascensionTable = Table()
		val slotStack = Stack()
		slotStack.add(SpriteWidget(AssetManager.loadSprite("Icons/icon_base"), 32f, 32f))
		slotStack.add(SpriteWidget(slot.icon.copy(), 32f, 32f))
		slotStack.addTapToolTip("${slot.name.neaten()} slot.")
		ascensionTable.add(slotStack)
		ascensionTable.add(
			SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = ascension.colour), 48f, 48f)
				.addTapToolTip("Ascension level " + (ascension.ordinal + 1)))
		ascensionTable.add(SpriteWidget(weight.icon.copy(), 32f, 32f).addTapToolTip("${weight.niceName} equipment."))
		ascensionTable.row()
		ascensionTable.add(Label(ascension.toString().neaten(), Global.skin).tint(ascension.colour.color())).colspan(3).center()

		table.add(ascensionTable).padBottom(5f)
		table.row()
		table.add(Label(fullName, Global.skin, "card").wrap().align(Align.center)).growX().center()
		table.row()

		val imageStack = Stack()
		imageStack.add(SpriteWidget(Sprite(fullIcon.glow), 64f, 64f).tint(Color(1f, 1f, 1f, 0.75f)))
		imageStack.add(MaskedTexture(fullIcon))
		table.add(imageStack).size(64f).expandX().center()
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
		addSubtextNumber("Rating", calculatePowerRating(entity).toInt())

		table.add(levelPowerTable).growX().pad(3f)
		table.row()

		table.add(Label(fullDescription, Global.skin, "card").wrap()).growX().center()
		table.row()

		var bright = true
		for (stat in Statistic.Values)
		{
			val statTable = Table()

			val rawValue = getStat(stat, entity?.stats()?.level ?: 1)
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
				rowTable.add(Label(stat.niceName, Global.skin, "cardsmall")).pad(5f)
				rowTable.add(Label(valueStr, Global.skin, "cardsmall")).expandX().right().pad(5f)
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