package com.lyeeedar.Game

import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

// ----------------------------------------------------------------------
enum class EquipmentWeight(val icon: Sprite, val niceName: String)
{
	HEAVY(AssetManager.loadSprite("Icons/EquipmentHeavy"), "Heavy"),
	MEDIUM(AssetManager.loadSprite("Icons/EquipmentMedium"), "Medium"),
	LIGHT(AssetManager.loadSprite("Icons/EquipmentLight"), "Light");

	companion object
	{
		val Values = EquipmentWeight.values()
	}
}