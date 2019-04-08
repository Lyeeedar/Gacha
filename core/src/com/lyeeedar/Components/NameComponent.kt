package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Util.XmlData

class NameComponent() : AbstractComponent()
{
	lateinit var name: String
	lateinit var title: String

	constructor(name: String) : this()
	{
		this.name = name
	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		name = xml.get("Name", "")!!
		title = xml.get("Title", "")!!
	}
}