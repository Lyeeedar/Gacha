package com.lyeeedar.Screens

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Game.EquipmentCreator
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.NavigationBar
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random

class ShopScreen : AbstractScreen()
{
	override fun create()
	{
		val shopTable = Table()

		val generateEquipmentButton = TextButton("Equipment", Global.skin)
		generateEquipmentButton.addClickListener {

			val equipment = Array<CardWidget>()
			for (i in 0 until 22)
			{
				val equip = EquipmentCreator.create(EquipmentSlot.Values.random(), EquipmentWeight.values().random(), Random.random(100), Ascension.values().random(), Random.random)
				val table = equip.createCardTable()

				val card = CardWidget(table, table, AssetManager.loadTextureRegion("white")!!, null)
				equipment.add(card)

				stage.addActor(card)
			}

			CardWidget.layoutCards(equipment, Direction.CENTER)
		}

		shopTable.add(generateEquipmentButton)

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.SHOP)).growX()
	}

	override fun doRender(delta: Float)
	{

	}
}