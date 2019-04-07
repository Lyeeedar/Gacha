package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.MainGame
import com.lyeeedar.UI.NavigationBar

class ShopScreen : AbstractScreen()
{
	override fun create()
	{
		val shopTable = Table()

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.SHOP)).growX().height(75f)
	}

	override fun doRender(delta: Float)
	{

	}
}