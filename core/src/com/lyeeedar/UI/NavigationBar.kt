package com.lyeeedar.UI

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap

class NavigationBar(val current: MainGame.ScreenEnum) : Table()
{
	val screenMap = FastEnumMap<MainGame.ScreenEnum, TextAndIconButton>(MainGame.ScreenEnum::class.java)

	override fun getHeight(): Float
	{
		return 50f
	}

	override fun getPrefHeight(): Float
	{
		return 50f
	}

	init
	{
		defaults().width(Value.percentWidth(1f / 5f, this)).growY()

		background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel"))

		val zoneButton = TextAndIconButton("Journey", AssetManager.loadSprite("GUI/EquipmentCardback"), Global.skin.getFont("small"))
		val shopButton = TextAndIconButton("Shop", AssetManager.loadSprite("GUI/MoneyCardback"), Global.skin.getFont("small"))
		val heroesButton = TextAndIconButton("Heroes", AssetManager.loadSprite("GUI/CharacterCardback"), Global.skin.getFont("small"))
		val settingsButton = TextAndIconButton("Settings", AssetManager.loadSprite("GUI/CardCardback"), Global.skin.getFont("small"))
		val questsButton = TextAndIconButton("Quests", AssetManager.loadSprite("GUI/ChanceCardback"), Global.skin.getFont("small"))

		screenMap[MainGame.ScreenEnum.ZONE] = zoneButton
		screenMap[MainGame.ScreenEnum.SHOP] = shopButton
		screenMap[MainGame.ScreenEnum.HEROES] = heroesButton
		screenMap[MainGame.ScreenEnum.SETTINGS] = settingsButton
		screenMap[MainGame.ScreenEnum.QUESTS] = questsButton

		screenMap[current].background = AssetManager.loadTextureRegion("GUI/BasePanelHighlight")

		for (screen in MainGame.ScreenEnum.values())
		{
			if (screen == current) continue
			val button = screenMap[screen] ?: continue

			button.addClickListener {
				Global.game.switchScreen(screen)
			}
		}

		add(shopButton)
		add(questsButton)
		add(zoneButton)
		add(heroesButton)
		add(settingsButton)
	}
}