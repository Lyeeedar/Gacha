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
				val part = EquipmentPart()
				part.parse(getXml(xml))
				parts.add(part)
			}

			loaded = true
		}

		fun create(slot: EquipmentSlot, weight: EquipmentWeight, level: Int, ascension: Ascension, ran: LightRNG): Equipment
		{
			loadParts()

			// load base equipment
			val baseEquipmentName = "Equipment/" + weight.toString().neaten() + slot.toString().neaten()
			val baseEquipment = Equipment.load(getXml(baseEquipmentName))

			val material = getPart(EquipmentPart.PartType.MATERIAL, slot, weight, level, ascension, ran)
			if (material != null)
			{
				applyPart(baseEquipment, material)
				baseEquipment.name = material.name + " " + baseEquipment.name

				if (material.layerTexture != null)
				{
					baseEquipment.icon.layer1 = material.layerTexture!!
				}
			}

			if (ran.nextFloat() < 0.3f * ascension.multiplier) // 30% chance of prefix
			{
				val prefix = getPart(EquipmentPart.PartType.PREFIX, slot, weight, level, ascension, ran)
				if (prefix != null)
				{
					applyPart(baseEquipment, prefix)
					baseEquipment.name = prefix.name + " " + baseEquipment.name

					if (prefix.layerTexture != null)
					{
						baseEquipment.icon.layer2 = prefix.layerTexture!!
					}
				}
			}

			if (ran.nextFloat() < 0.1f * ascension.multiplier) // 10% chance of suffix
			{
				val suffix = getPart(EquipmentPart.PartType.SUFFIX, slot, weight, level, ascension, ran)
				if (suffix != null)
				{
					applyPart(baseEquipment, suffix)
					baseEquipment.name = baseEquipment.name + " " + suffix.name

					if (suffix.layerTexture != null)
					{
						baseEquipment.icon.layer3 = suffix.layerTexture!!
					}
				}
			}

			return baseEquipment
		}

		private fun applyPart(equipment: Equipment, part: Part)
		{
			for (stat in Statistic.Values)
			{
				val partVal = part.stats[stat] ?: 0f
				val equipVal = equipment.stats[stat] ?: 0f

				equipment.stats[stat] = partVal + equipVal
			}

			for (event in EventType.values())
			{
				val equipEvents = equipment.eventHandlers[event] ?: Array()
				val partEvents = part.eventHandlers[event] ?: Array()

				equipEvents.addAll(partEvents)

				equipment.eventHandlers[event] = equipEvents
			}
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
						for (i in 0 until ep.rarity.weight)
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
	}
}

class EquipmentPart
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
		for (partEl in partsEl.children)
		{
			val part = Part()
			part.parse(partEl)

			parts.add(part)
		}
	}
}

class Part
{
	lateinit var name: String

	var minLevel: Int = 1
	lateinit var minAscension: Ascension

	var layerTexture: TextureRegion? = null

	val stats = FastEnumMap<Statistic, Float>(Statistic::class.java)
	val eventHandlers = FastEnumMap<EventType, Array<EventAndCondition>>(EventType::class.java)

	fun parse(xmlData: XmlData)
	{
		name = xmlData.get("Name")
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