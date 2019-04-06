package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.Zone
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.ZoneWidget
import com.lyeeedar.UI.addClickListener

class ZoneScreen : AbstractScreen()
{
	override fun create()
	{
		val zone = Zone.load("Zones/Test")
		val widget = ZoneWidget(zone)

		mainTable.background = TiledDrawable(TextureRegionDrawable(zone.theme.backgroundTile)).tint(zone.theme.ambient.color())

		mainTable.add(widget).grow()
		mainTable.row()

		val beginButton = TextButton("Begin", Global.skin)
		beginButton.addClickListener {
			val enc = zone.currentEncounter.encounter!!

			val level = enc.createLevel(zone.theme)
			val mapScreen = Global.game.getTypedScreen<MapScreen>()!!
			mapScreen.setNewLevel(zone)

			Global.game.switchScreen(MainGame.ScreenEnum.MAP)

//			zone.currentEncounter.isComplete = true
//			zone.currentEncounter.isCurrent = false
//			zone.currentEncounter.updateFlag()
//
//			zone.currentEncounter = zone.currentEncounter.nextTile!!
//			zone.currentEncounter.isCurrent = true
//			zone.currentEncounter.updateFlag()
		}

		mainTable.add(beginButton).pad(10f)
	}

	override fun doRender(delta: Float)
	{

	}
}