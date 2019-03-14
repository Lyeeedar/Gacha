package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Util.XmlData

class OccludesComponent : AbstractComponent()
{
	var occludes = true

	override fun parse(xml: XmlData, entity: Entity)
	{
		occludes = xml.getBoolean("Occludes", true)
	}
}