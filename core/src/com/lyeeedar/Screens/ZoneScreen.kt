package com.lyeeedar.Screens

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
	val gameDataBar = GameDataBar()

	override fun create()
	{
		drawFPS = false
		updateTable()
	}

	fun updateTable()
	{
		mainTable.clear()

		val zone = Zone.load(Global.data.currentZone)
		val widget = ZoneWidget(zone)

		mainTable.background = TiledDrawable(TextureRegionDrawable(zone.floor1.sprite!!.currentTexture))

		mainTable.add(gameDataBar).growX()
		mainTable.row()

		widget.width = stage.width

		mainTable.add(widget).grow()
		mainTable.row()

		if (Global.data.currentZoneProgression == Zone.numEncounters)
		{
			val nextZoneButton = TextButton("Journey Onwards", Global.skin)
			nextZoneButton.addClickListener {
				Global.data.currentZone++
				Global.data.currentZoneProgression = 0

				updateTable()
			}

			mainTable.add(nextZoneButton).expandX().center().width(200f).height(30f).pad(5f)
			mainTable.row()
		}
		else
		{
			val beginButton = TextButton("Begin", Global.skin)
			beginButton.addClickListener {
				val mapScreen = Global.game.getTypedScreen<MapScreen>()!!
				mapScreen.setNewLevel(zone)

				Global.game.switchScreen(MainGame.ScreenEnum.MAP)
			}

			mainTable.add(beginButton).expandX().center().width(200f).height(30f).pad(5f)
			mainTable.row()
		}

		mainTable.add(NavigationBar(MainGame.ScreenEnum.ZONE)).growX()
	}

	override fun show()
	{
		super.show()

		updateTable()
		gameDataBar.rebind()
	}

	override fun doRender(delta: Float)
	{

	}
}