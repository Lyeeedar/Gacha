package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.UI.NavigationBar
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random

class QuestsScreen : AbstractScreen()
{
	val gameDataBar = GameDataBar()

	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.7f, 0.7f, 0.7f, 1f))

		createHubTable()
	}

	fun createHubTable()
	{
		val table = Table()

		val bountyTable = Table()

		val bountyBoard = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/bountyboard")!!), 128f, 64f)
		bountyTable.add(bountyBoard)
		bountyTable.row()
		bountyTable.add(Label("Bounties", Global.skin))

		table.add(bountyTable).expand().top().left().pad(5f)
		table.row()

		for (i in 0 until 4)
		{
			// do tables
			val aboveTable = Table()
			val leftTable = Table()
			val rightTable = Table()
			val belowTable = Table()
			val tableTable = Table()

			val tableSprite = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/roundedtable")!!), 48f, 48f)

			tableTable.add(aboveTable).colspan(3).height(24f)
			tableTable.row()
			tableTable.add(leftTable).height(24f)
			tableTable.add(tableSprite).height(24f)
			tableTable.add(rightTable).height(24f)
			tableTable.row()
			tableTable.add(belowTable).colspan(3).height(24f)

			// add between 2 and 5 heroes
			var addedLeft = false
			var addedRight = false
			val numHeroes = Random.random(3) + 2
			for (i in 0 until numHeroes)
			{
				val validPositions = com.badlogic.gdx.utils.Array<Direction>()
				validPositions.add(Direction.NORTH)
				validPositions.add(Direction.SOUTH)
				if (!addedLeft) validPositions.add(Direction.WEST)
				if (!addedRight) validPositions.add(Direction.EAST)

				val chosenPos = validPositions.random()
				val targetTable: Table
				if (chosenPos == Direction.NORTH)
				{
					targetTable = aboveTable
				}
				else if (chosenPos == Direction.SOUTH)
				{
					targetTable = belowTable
				}
				else if (chosenPos == Direction.WEST)
				{
					targetTable = leftTable
					addedLeft = true
				}
				else if (chosenPos == Direction.EAST)
				{
					targetTable = rightTable
					addedRight = true
				}
				else
				{
					throw Exception("Unhandled direction")
				}

				val potentialCharacters = arrayOf(
					"Oryx/Custom/heroes/bar_patron",
					"Oryx/Custom/heroes/bar_patron_f",
					"Oryx/Custom/heroes/barmaid",
					"Oryx/Custom/heroes/innkeep",
					"Oryx/Custom/heroes/peasant_f"
												 )

				val selected = potentialCharacters.random()
				val sprite = AssetManager.loadSprite(selected, drawActualSize = true)
				sprite.randomiseAnimation()
				val widget = SpriteWidget(sprite, 48f, 48f)

				targetTable.add(widget)
			}

			val alignments = arrayOf(Align.left, Align.right)
			table.add(tableTable).pad(5f).padLeft(20f + Random.random(240f)).padRight(20f + Random.random(240f)).expand().align(alignments.random())
			table.row()
		}

		val wallTable = Table()
		wallTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/wall_dungeon_14")))

		mainTable.clear()
		mainTable.add(gameDataBar).growX()
		mainTable.row()
		mainTable.add(wallTable).height(48f).growX()
		mainTable.row()
		mainTable.add(table).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.QUESTS)).growX()
	}

	override fun show()
	{
		gameDataBar.rebind()
		super.show()
	}

	override fun doRender(delta: Float)
	{

	}
}