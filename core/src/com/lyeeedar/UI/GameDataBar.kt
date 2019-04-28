package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Components.stats
import com.lyeeedar.Game.Zone
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.prettyPrint

class GameDataBar : Table()
{
	init
	{
		rebind()
	}

	override fun act(delta: Float)
	{
		if (currentlyBound != this)
		{
			rebind()
			currentlyBound = this
		}

		goldLabel.value = Global.data.gold
		expLabel.value = Global.data.experience

		super.act(delta)
	}

	fun rebind()
	{
		clear()

		background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).setNoMinSize()

		val zoneProgress = PercentageBarWidget(Global.data.currentZoneProgression.toFloat() / Zone.numEncounters, Colour.GOLD, Zone.numEncounters)

		val zoneLabel = Label(Global.data.currentZone.toString() + "-" + (Global.data.currentZoneProgression+1), Global.skin)
		val zoneTable = Table()
		zoneTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		zoneTable.add(zoneLabel).expandX().left().padLeft(3f)

		val rating = Global.data.heroPool.map { it.getEntity("1") }.map { it.stats().calculatePowerRating(it) }.sortedByDescending { it }.take(5).sum()
		val ratingLabel = Label(rating.toInt().prettyPrint(), Global.skin)
		ratingLabel.color = Color.GOLD

		val ratingTable = Table()
		ratingTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		ratingTable.add(SpriteWidget(Sprite(AssetManager.loadTextureRegion("Icons/upgrade")!!), 16f, 16f).tint(Color.GOLD)).padRight(3f)
		ratingTable.add(ratingLabel).expandX().left()

		val goldTable = Table()
		goldTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		goldTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 24f, 24f))
		goldTable.add(goldLabel).expandX().left()

		val expTable = Table()
		expTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		expTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_blue"), 24f, 24f))
		expTable.add(expLabel).expandX().left()

		add(zoneTable).pad(3f).width(Value.percentWidth(0.2f, this)).height(18f)
		add(ratingTable).pad(3f).width(Value.percentWidth(0.23f, this)).height(18f)
		add(goldTable).pad(3f).width(Value.percentWidth(0.23f, this)).height(18f)
		add(expTable).pad(3f).width(Value.percentWidth(0.23f, this)).height(18f)

		row()

		add(zoneProgress).colspan(4).growX().height(5f).padBottom(3f)
	}

	companion object
	{
		var currentlyBound: GameDataBar? = null

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