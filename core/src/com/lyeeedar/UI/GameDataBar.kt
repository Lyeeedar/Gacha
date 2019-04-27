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

		add(goldLabel)
		add(expLabel)
	}

	companion object
	{
		val goldLabel = NumberChangeLabel("Gold", Global.skin)
		val expLabel = NumberChangeLabel("Exp", Global.skin)

		init
		{
			goldLabel.value = Global.data.gold
			expLabel.value = Global.data.experience

			goldLabel.complete()
			expLabel.complete()
		}
	}
}