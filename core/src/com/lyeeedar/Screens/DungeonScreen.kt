package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.MainGame
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.UI.NavigationBar

class DungeonScreen : AbstractScreen()
{
	override fun create()
	{
		val dungeonTable = Table()

		mainTable.add(GameDataBar()).growX()
		mainTable.row()
		mainTable.add(dungeonTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.DUNGEON)).growX()
	}

	override fun doRender(delta: Float)
	{

	}
}