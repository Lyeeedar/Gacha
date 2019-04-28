package com.lyeeedar.Game

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.EventAndCondition
import com.lyeeedar.Util.*
import squidpony.squidmath.LightRNG

class EquipmentCreator
{
	companion object
	{
		private val parts = Array<EquipmentPart>()
		private var loaded = false
		private fun loadParts()
		{
			if (loaded) return

			// find all relevant parts files
			for (xml in XmlData.enumeratePaths("EquipmentParts", "EquipmentPart"))
			{
				val part = EquipmentPart(xml)
				part.parse(getXml(xml))
				parts.add(part)
			}

			loaded = true
		}

		fun createRandom(level: Int, ran: LightRNG = Random.random, slot: EquipmentSlot? = null, weight: EquipmentWeight? = null): Equipment
		{
			val slot = slot ?: EquipmentSlot.Values.random(ran)
			val weight = weight ?: EquipmentWeight.Values.random(ran)
			val ascension = Ascension.getWeightedAscension(ran)

			return create(slot, weight, level, ascension, ran)
		}

		fun create(slot: EquipmentSlot, weight: EquipmentWeight, level: Int, ascension: Ascension, ran: LightRNG): Equipment
		{
			loadParts()

			// load base equipment
			val baseEquipmentName = "Equipment/" + weight.toString().neaten() + slot.toString().neaten()
			val baseEquipment = Equipment(baseEquipmentName)
			baseEquipment.parse(getXml(baseEquipmentName))
			baseEquipment.ascension = ascension

			baseEquipment.material = getPart(EquipmentPart.PartType.MATERIAL, slot, weight, level, ascension, ran)

			if (ran.nextFloat() < 0.3f * ascension.multiplier) // 30% chance of prefix
			{
				baseEquipment.prefix = getPart(EquipmentPart.PartType.PREFIX, slot, weight, level, ascension, ran)
			}

			if (ran.nextFloat() < 0.1f * ascension.multiplier) // 10% chance of suffix
			{
				baseEquipment.suffix = getPart(EquipmentPart.PartType.SUFFIX, slot, weight, level, ascension, ran)
			}

			return baseEquipment
		}

		private fun getPart(partType: EquipmentPart.PartType, slot: EquipmentSlot, weight: EquipmentWeight, level: Int, ascension: Ascension, ran: LightRNG): Part?
		{
			// get all relevant parts
			val validParts = Array<Part>()
			for (ep in parts)
			{
				if (ep.type == partType && ep.allowedSlots.contains(slot) && ep.allowedWeights.contains(weight))
				{
					// valid type, check we have something in our level range
					var part: Part? = null
					for (p in ep.parts)
					{
						if (p.minLevel <= level && p.minAscension.ordinal <= ascension.ordinal)
						{
							part = p
						}
					}

					if (part != null)
					{
						for (i in 0 until ep.rarity.dropRate)
						{
							validParts.add(part)
						}
					}
				}
			}

			if (validParts.size == 0)
			{
				return null
			}
			else
			{
				return validParts.random(ran)
			}
		}

		fun getPart(path: String): Part?
		{
			val split = path.split(':')
			val partPath = split[0]
			val index = split[1].toInt()

			val ep = parts.firstOrNull { it.loadPath == partPath } ?: return null
			return ep.parts[index]
		}
	}
}

class EquipmentPart(var loadPath: String)
{
	enum class PartType
	{
		PREFIX,
		MATERIAL,
		SUFFIX
	}

	lateinit var type: PartType
	val allowedSlots = Array<EquipmentSlot>()
	val allowedWeights = Array<EquipmentWeight>()
	lateinit var rarity: Rarity

	val parts = Array<Part>()

	fun parse(xmlData: XmlData)
	{
		type = PartType.valueOf(xmlData.get("Type", "Material")!!.toUpperCase())

		val rawAllowedSlots = xmlData.get("AllowedSlots").split(",")
		for (slot in rawAllowedSlots)
		{
			allowedSlots.add(EquipmentSlot.valueOf(slot.toUpperCase()))
		}

		val rawAllowedWeights = xmlData.get("AllowedWeights").split(",")
		for (weight in rawAllowedWeights)
		{
			allowedWeights.add(EquipmentWeight.valueOf(weight.toUpperCase()))
		}

		rarity = Rarity.valueOf(xmlData.get("Rarity", "Common")!!.toUpperCase())

		val partsEl = xmlData.getChildByName("Parts")!!
		var i = 0
		for (partEl in partsEl.children)
		{
			val part = Part("$loadPath:$i")
			part.parse(partEl)

			parts.add(part)

			i++
		}
	}
}

class Part(override var loadPath: String) : IEquipmentStatsProvider
{
	override lateinit var name: String
	override lateinit var description: String

	var minLevel: Int = 1
	lateinit var minAscension: Ascension

	var layerTexture: TextureRegion? = null

	override val stats = FastEnumMap<Statistic, Float>(Statistic::class.java)
	override val eventHandlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name", "")!!
		description = xmlData.get("Description", "")!!

		minLevel = xmlData.getInt("MinLevel", 1)
		minAscension = Ascension.valueOf(xmlData.get("MinAscension", "mundane")!!.toUpperCase())

		val layerTextureEl = xmlData.getChildByName("Layer")
		if (layerTextureEl != null)
		{
			layerTexture = AssetManager.loadTextureRegion(layerTextureEl.get("File"))
		}

		Statistic.parse(xmlData.getChildByName("Statistics"), stats)
		EventType.parseEvents(xmlData.getChildByName("EventHandlers"), eventHandlers)
	}
}