package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager

class GameDataBar : Table()
{
	init
	{
		rebind()
	}

	override fun act(delta: Float)
	{
		goldLabel.value = Global.data.gold
		expLabel.value = Global.data.experience

		super.act(delta)
	}

	override fun getHeight(): Float
	{
		return 25f
	}

	override fun getPrefHeight(): Float
	{
		return 25f
	}

	fun rebind()
	{
		clear()

		background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.5f, 0.5f, 0.5f, 1f))

		val goldTable = Table()
		goldTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 24f, 24f))
		goldTable.add(goldLabel)

		val expTable = Table()
		expTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_blue"), 24f, 24f))
		expTable.add(expLabel)

		add(goldTable).pad(5f)
		add(expTable).pad(5f)
	}

	companion object
	{
		val goldLabel = NumberChangeLabel("", Global.skin)
		val expLabel = NumberChangeLabel("", Global.skin)

		init
		{
			goldLabel.value = Global.data.gold
			expLabel.value = Global.data.experience

			goldLabel.complete()
			expLabel.complete()
		}
	}
}