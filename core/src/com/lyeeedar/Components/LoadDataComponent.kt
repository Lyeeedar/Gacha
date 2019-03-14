package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Util.XmlData

class LoadDataComponent(val xml: XmlData) : AbstractComponent()
{
	init
	{
		fromLoad = true
	}

	override fun parse(xml: XmlData, entity: Entity)
	{

	}

}