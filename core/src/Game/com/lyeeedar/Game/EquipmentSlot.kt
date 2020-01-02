package com.lyeeedar

import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

// ----------------------------------------------------------------------
enum class EquipmentSlot(val icon: Sprite)
{
	HEAD(AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_plate_helm")),
	WEAPON(AssetManager.loadSprite("Oryx/uf_split/uf_items/weapon_broadsword")),
	BODY(AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_plate_chest")),
	FEET(AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_plate_boot"));

	companion object
	{
		val Values = EquipmentSlot.values()
	}
}