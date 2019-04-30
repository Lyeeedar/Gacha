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
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.random
import com.lyeeedar.Util.removeRandom
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

		val bountyTable = Table()

		val bountyBoard = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/bountyboard")!!), 128f, 64f)
		bountyTable.add(bountyBoard)
		bountyTable.row()
		bountyTable.add(Label("Bounties", Global.skin))

		table.add(bountyTable).expand().top().left().pad(5f)
		table.row()

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

		for (i in 0 until 4)
		{
			val numInRow = Random.random(3) + 1
			val space = ((stage.width - 20) / numInRow) - (32f * 3 + 3f * 3)
			val rowTable = Table()

			for (x in 0 until numInRow)
			{
				// do tables
				val aboveTable = Table()
				val leftTable = Table()
				val rightTable = Table()
				val belowTable = Table()
				val tableTable = Table()

				val tableSprite = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/roundedtable")!!, drawActualSize = true), 48f, 48f)

				val tableStack = Stack()
				tableStack.add(tableSprite)

				if (potentialDecorations.size > 0 && Random.random() < 0.6f) // 60% chance at decoration
				{
					val decorationSprite = Sprite(AssetManager.loadTextureRegion(potentialDecorations.removeRandom())!!, drawActualSize = true)
					tableStack.add(SpriteWidget(decorationSprite, 48f, 48f))
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

					targetTable.add(widget)
				}

				rowTable.add(tableTable).padLeft(Random.random(space)).padBottom(Random.random(25f)).expand().left().bottom()
			}

			table.add(rowTable).growX().pad(10f)
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