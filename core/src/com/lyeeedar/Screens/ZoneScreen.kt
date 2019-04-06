package com.lyeeedar.Screens

import com.lyeeedar.Game.Zone
import com.lyeeedar.UI.ZoneWidget

class ZoneScreen : AbstractScreen()
{
	override fun create()
	{
		val zone = Zone.load("Zones/Test")
		val widget = ZoneWidget(zone)

		mainTable.add(widget).grow()
	}

	override fun doRender(delta: Float)
	{

	}
}