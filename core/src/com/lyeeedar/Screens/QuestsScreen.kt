package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Components.renderable
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.GameDataBar
import com.lyeeedar.UI.NavigationBar
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.Util.*
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray

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

		val fixedContent = kotlin.Array<Table?>(5) { null }

		val bountyTable = Table()
		val bountyBoard = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/bountyboard")!!), 128f, 64f)
		bountyTable.add(bountyBoard)
		bountyTable.row()
		bountyTable.add(Label("Bounties", Global.skin)).padBottom(16f)
		fixedContent[0] = bountyTable

		val expeditionTable = Table()
		val expeditionBoard = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/expedition")!!), 128f, 64f)
		expeditionTable.add(expeditionBoard)
		expeditionTable.row()
		expeditionTable.add(Label("Expedition", Global.skin)).padBottom(16f)
		fixedContent[2] = expeditionTable

		val heroPool = Global.data.heroPool.toGdxArray()

		val potentialDecorations = gdxArrayOf(
			"Oryx/Custom/terrain/roundedtable_decoration1",
			"Oryx/Custom/terrain/roundedtable_decoration2",
			"Oryx/Custom/terrain/roundedtable_decoration3",
			"Oryx/Custom/terrain/roundedtable_decoration4",
			"Oryx/Custom/terrain/roundedtable_decoration5",
			"Oryx/Custom/terrain/roundedtable_decoration6",
			"Oryx/Custom/terrain/roundedtable_decoration7",
			"Oryx/Custom/terrain/roundedtable_decoration8"

										  )

		val tableHeight = 32f * 2 + 3f * 2
		var verticalSpace = stage.height - (gameDataBar.prefHeight + 48f + 50f)
		for (i in 0 until 5)
		{
			var rowHeight = tableHeight

			val fixedContentTable = fixedContent[i]
			if (fixedContentTable != null)
			{
				val contentHeight = fixedContentTable.prefHeight + 10f
				rowHeight = max(rowHeight, contentHeight)
			}

			verticalSpace -= rowHeight
		}

		var left = true
		for (i in 0 until 5)
		{
			val fullRowTable = Table()
			val rowTable = Table()
			val fixedTable = Table()

			if (left)
			{
				fullRowTable.add(fixedTable).growY()
				fullRowTable.add(rowTable).grow()
			}
			else
			{
				fullRowTable.add(rowTable).grow()
				fullRowTable.add(fixedTable).growY()
			}

			table.add(fullRowTable).growX()
			table.row()


			var rowWidth = stage.width

			// get fixed content
			val fixedContentTable = fixedContent[i]
			if (fixedContentTable != null)
			{
				fixedTable.add(fixedContentTable).grow().pad(5f)
				rowWidth -= fixedContentTable.prefWidth + 10f

				left = !left
			}

			val tableMaxSize = 32f * 3 + 3f * 3 + 10f
			val maxTables = (rowWidth / tableMaxSize).toInt()
			val halfFloat = maxTables / 2f
			val halfMin = halfFloat.floor()
			val halfMax = halfFloat.ciel()

			val numInRow = Random.random(halfMin+1) + halfMax
			val space = (rowWidth / numInRow) - (tableMaxSize)

			val rowVertPad = max(verticalSpace / (5 - i), (fixedContentTable?.prefHeight ?: 0f) - tableHeight)
			var maxRowVertPad = 0f
			for (x in 0 until numInRow)
			{
				// do tables
				val aboveTable = Table()
				val leftTable = Table()
				val rightTable = Table()
				val belowTable = Table()
				val tableTable = Table()

				val tableColor = Color(0.8f, 0.8f, 0.8f, 1f)

				val tableSprite = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/roundedtable")!!, drawActualSize = true), 48f, 48f)
				tableSprite.color = tableColor

				val tableStack = Stack()
				tableStack.add(tableSprite)

				if (potentialDecorations.size > 0 && Random.random() < 0.6f) // 60% chance at decoration
				{
					val decorationSprite = Sprite(AssetManager.loadTextureRegion(potentialDecorations.removeRandom())!!, drawActualSize = true)
					val decorationWidget = SpriteWidget(decorationSprite, 48f, 48f)
					decorationWidget.color = tableColor
					tableStack.add(decorationWidget)
				}

				tableTable.add(aboveTable).colspan(3).height(32f)
				tableTable.row()
				tableTable.add(leftTable).height(32f).width(32f).padRight(3f)
				tableTable.add(tableStack).height(32f).width(32f)
				tableTable.add(rightTable).height(32f).width(32f).padLeft(3f)


				// add between 2 and 4 heroes
				var addedLeft = false
				var addedRight = false
				val numHeroes = Random.random(2) + 2
				var lastSelected: String = ""
				for (i in 0 until numHeroes)
				{
					val validPositions = com.badlogic.gdx.utils.Array<Direction>()
					validPositions.add(Direction.NORTH)
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
						"Oryx/Custom/heroes/peasant_f",
						"Oryx/Custom/heroes/peasant_m",
						"Oryx/Custom/heroes/peasant_f2",
						"Oryx/Custom/heroes/peasant_m2",
						"Oryx/Custom/heroes/merchant1",
						"---",
						"---",
						"---",
						"---",
						"---"
													 )

					var selected = potentialCharacters.random()
					while (selected == lastSelected || (selected == "---" && heroPool.size == 0))
					{
						selected = potentialCharacters.random()
					}
					lastSelected = selected

					val sprite: Sprite
					if (selected == "---")
					{
						// select random hero
						val hero = heroPool.removeRandom()
						val entity = hero.getEntity("1")
						sprite = (entity.renderable().renderable as Sprite).copy()
					}
					else
					{
						sprite = AssetManager.loadSprite(selected, drawActualSize = true)
					}

					sprite.randomiseAnimation()
					val widget = SpriteWidget(sprite, 48f, 48f)
					widget.color = tableColor

					targetTable.add(widget)
				}

				val vertPad = Random.random(rowVertPad)
				rowTable.add(tableTable).padLeft(Random.random(space) + 5f).padBottom(vertPad).expand().left().bottom()

				maxRowVertPad = max(maxRowVertPad, vertPad)
			}

			verticalSpace -= maxRowVertPad
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