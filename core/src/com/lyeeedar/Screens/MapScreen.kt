package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Game.Level
import com.lyeeedar.Global
import com.lyeeedar.Systems.render
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

		val mapWidget = Table()
		Global.engine.render().renderArea = mapWidget

		mainTable.add(mapWidget).grow()
	}

	override fun doRender(delta: Float)
	{

	}
}