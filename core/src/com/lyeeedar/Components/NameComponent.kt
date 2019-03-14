package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Util.XmlData

class NameComponent() : AbstractComponent()
{
	lateinit var name: String
	var isPlayer = false

	constructor(name: String) : this()
	{
		this.name = name
	}

	override fun parse(xml: XmlData, entity: Entity)
	{
		name = xml.get("Name")
	}
}