package com.lyeeedar.Game

import com.lyeeedar.Ascension
import com.lyeeedar.Util.Colour

// ----------------------------------------------------------------------
enum class Rarity private constructor(val niceName: String, val spawnWeight: Int, val dropRate: Int, val shortName: String, val colour: Colour, val ascension: Ascension)
{
	COMMON("Common", 4, 5, "C", Colour(182, 157, 135, 255, true), Ascension.MUNDANE),
	UNCOMMON("Uncommon", 3, 4, "UC", Colour(98, 192, 87, 255, true), Ascension.EXCEPTIONAL),
	RARE("Rare", 1, 3, "R", Colour(40, 229, 255, 255, true), Ascension.EXTRAORDINARY),
	SUPERRARE("Super Rare", 1, 2, "SR", Colour(255, 239, 20, 255, true), Ascension.LEGENDARY),
	ULTRARARE("Ultra Rare", 1, 1, "UR", Colour(246, 20, 71, 255, true), Ascension.MYTHICAL);

	companion object
	{
		val Values = Rarity.values()
	}
}