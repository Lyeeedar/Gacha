package com.lyeeedar.Screens

import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.UI.RenderSystemWidget
import com.lyeeedar.Util.Colour

class MapScreen : AbstractScreen()
{
	var level: Level? = null

	override fun create()
	{
		level = Level.load("Levels/Test")
		Global.changeLevel(level!!, Colour.GOLD)

		//val collisionGrid = Array2D<Boolean>(map.width, map.height) { x,y -> map.grid[x,y].type == TileType.WALL }
		//Global.collisionGrid = collisionGrid

		val mapWidget = RenderSystemWidget()

		mainTable.debug()
		mainTable.add(mapWidget).grow()
	}

	override fun doRender(delta: Float)
	{
		Global.engine.update(delta)
	}
}