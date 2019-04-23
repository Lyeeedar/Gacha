package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.Zone
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.UI.NavigationBar
import com.lyeeedar.UI.ZoneWidget
import com.lyeeedar.UI.addClickListener

class ZoneScreen : AbstractScreen()
{
	override fun create()
	{
		val zone = Zone.load("Zones/Test")
		val widget = ZoneWidget(zone)

		mainTable.background = TiledDrawable(TextureRegionDrawable(zone.theme.backgroundTile)).tint(zone.theme.ambient.color())

		mainTable.add(GameDataBar()).growX()
		mainTable.row()

		val stack = Stack()
		widget.width = stage.width

		stack.add(widget)

		mainTable.add(stack).grow()
		mainTable.row()

		val beginButton = TextButton("Begin", Global.skin)
		beginButton.addClickListener {
			val mapScreen = Global.game.getTypedScreen<MapScreen>()!!
			mapScreen.setNewLevel(zone)

			Global.game.switchScreen(MainGame.ScreenEnum.MAP)
		}

		val buttonsTable = Table()
		stack.add(buttonsTable)

		buttonsTable.add(beginButton).pad(20f).expand().bottom().width(200f).height(30f)
		buttonsTable.row()

		buttonsTable.add(NavigationBar(MainGame.ScreenEnum.ZONE)).growX()
	}

	override fun doRender(delta: Float)
	{

	}
}