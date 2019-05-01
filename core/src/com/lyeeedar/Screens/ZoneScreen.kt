package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.lyeeedar.Game.Zone
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.*

class ZoneScreen : AbstractScreen()
{
	val gameDataBar = GameDataBar()
	val navigationBar = NavigationBar(MainGame.ScreenEnum.ZONE)

	var zoneCreatedFrom: Int = -1
	lateinit var zone: Zone

	override fun create()
	{
		drawFPS = false
		updateTable()

		if (!Global.release)
		{
			debugConsole.register("setzone", "", { args, console ->
				val zone = args[0].toInt()
				val progression = args[1].toInt()

				Global.data.currentZone = zone
				Global.data.currentZoneProgression = progression

				updateTable()

				true
			})
		}
	}

	fun updateTable()
	{
		if (Global.data.currentZone != zoneCreatedFrom)
		{
			zone = Zone.load(Global.data.currentZone)
			zoneCreatedFrom = Global.data.currentZone
		}

		mainTable.clear()

		val widget = ZoneWidget(zone)

		mainTable.add(gameDataBar).growX()
		mainTable.row()

		widget.width = stage.width

		val zoneStack = Stack()
		zoneStack.add(widget)

		if (Global.data.currentZoneProgression == Zone.numEncounters)
		{
			val nextZoneButton = TextButton("Journey Onwards", Global.skin)
			nextZoneButton.addClickListener {
				Global.data.currentZone++
				Global.data.currentZoneProgression = 0

				updateTable()
			}

			zoneStack.addTable(nextZoneButton).expand().center().bottom().width(200f).height(30f).pad(5f)
		}
		else
		{
			val beginButton = TextButton("Begin", Global.skin)
			beginButton.addClickListener {
				val mapScreen = Global.game.getTypedScreen<MapScreen>()!!
				mapScreen.setNewLevel(zone)

				Global.game.switchScreen(MainGame.ScreenEnum.MAP)
			}

			zoneStack.addTable(beginButton).expand().center().bottom().width(200f).height(30f).pad(5f)
		}

		mainTable.add(zoneStack).grow()
		mainTable.row()

		mainTable.add(navigationBar).growX()
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