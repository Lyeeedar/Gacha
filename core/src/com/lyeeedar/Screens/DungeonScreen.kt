package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.MainGame
import com.lyeeedar.UI.NavigationBar

class DungeonScreen : AbstractScreen()
{
	override fun create()
	{
		val shopTable = Table()

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.DUNGEON)).growX().height(75f)
	}

	override fun doRender(delta: Float)
	{

	}
}