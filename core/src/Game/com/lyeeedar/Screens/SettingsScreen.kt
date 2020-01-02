package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.MainGame
import com.lyeeedar.UI.NavigationBar
import com.lyeeedar.Util.Statics

class SettingsScreen : AbstractScreen()
{
	override fun create()
	{
		val shopTable = Table()

		shopTable.add(Label("Work in Progress", Statics.skin))

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.SETTINGS)).growX()
	}

	override fun doRender(delta: Float)
	{

	}
}