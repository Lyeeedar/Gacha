package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.MainGame
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.UI.NavigationBar

class DungeonScreen : AbstractScreen()
{
	val gameDataBar = GameDataBar()

	override fun create()
	{
		val dungeonTable = Table()

		mainTable.add(gameDataBar).growX()
		mainTable.row()
		mainTable.add(dungeonTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.DUNGEON)).growX()
	}

	override fun show()
	{
		gameDataBar.rebind()
		super.show()
	}

	override fun doRender(delta: Float)
	{

	}
}