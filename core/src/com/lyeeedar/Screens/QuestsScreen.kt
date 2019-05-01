package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.renderable
import com.lyeeedar.Direction
import com.lyeeedar.Game.AbstractBounty
import com.lyeeedar.Game.Save
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.collections.gdxArrayOf
import ktx.collections.toGdxArray
import java.lang.System.currentTimeMillis

class QuestsScreen : AbstractScreen()
{
	val borderedCircle = AssetManager.loadTextureRegion("borderedcircle")!!
	val notificationImg = AssetManager.loadTextureRegion("Icons/generic_notification")!!
	val redColour = Colour(0.85f, 0f, 0f, 1f)

	val navigationBar = NavigationBar(MainGame.ScreenEnum.QUESTS)

	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.7f, 0.7f, 0.7f, 1f))

		createHubTable()

		if (!Global.release)
		{
			debugConsole.register("clearbounties", "", { args, console ->

				Global.data.bounties.clear()
				createHubTable()

				true
			})

			debugConsole.register("completebounties", "", { args, console ->

				for (bounty in Global.data.bounties)
				{
					bounty.durationMillis = 0
				}
				createHubTable()

				true
			})
		}
	}

	fun createHubTable()
	{
		val gameDataBar = GameDataBar()
		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.7f, 0.7f, 0.7f, 1f))

		val ran = Random.obtainTS(Global.data.lastRefreshTime)

		val table = Table()

		val fixedContent = kotlin.Array<Table?>(5) { null }

		val bountyTable = Table()
		val bountyBoard = SpriteWidget(Sprite(AssetManager.loadTextureRegion("Oryx/Custom/terrain/bountyboard")!!), 128f, 64f)
		bountyTable.add(bountyBoard)
		bountyTable.row()

		val bountyLabelTable = Table()
		bountyLabelTable.add(Label("Bounties", Global.skin))

		if (Global.data.bounties.any{ it.isComplete() } || Global.data.bounties.size < Global.data.getAllowedNumBounties())
		{
			val notificationStack = Stack()
			notificationStack.add(SpriteWidget(Sprite(borderedCircle, colour = redColour), 12f, 12f))
			notificationStack.add(SpriteWidget(Sprite(notificationImg), 12f, 12f))

			bountyLabelTable.add(notificationStack).size(12f).expand().right().bottom().pad(2f)
		}

		bountyTable.add(bountyLabelTable).padBottom(16f)
		bountyTable.addClickListener {
			createBountiesTable()
		}
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

			val numInRow = ran.nextInt(halfMin+1) + halfMax
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

				if (potentialDecorations.size > 0 && ran.nextFloat() < 0.6f) // 60% chance at decoration
				{
					val decorationSprite = Sprite(AssetManager.loadTextureRegion(potentialDecorations.removeRandom(ran))!!, drawActualSize = true)
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
				val validPositions = com.badlogic.gdx.utils.Array<Direction>()
				validPositions.add(Direction.NORTH)
				validPositions.add(Direction.NORTH)
				validPositions.add(Direction.WEST)
				validPositions.add(Direction.EAST)

				val numHeroes = ran.nextInt(2) + 2
				var lastSelected: String = ""
				for (i in 0 until numHeroes)
				{
					val chosenPos = validPositions.removeRandom(ran)
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
					}
					else if (chosenPos == Direction.EAST)
					{
						targetTable = rightTable
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

					var selected = potentialCharacters.random(ran)
					while (selected == lastSelected || (selected == "---" && heroPool.size == 0))
					{
						selected = potentialCharacters.random(ran)
					}
					lastSelected = selected

					val sprite: Sprite
					if (selected == "---")
					{
						// select random hero
						val hero = heroPool.removeRandom(ran)
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

				val vertPad = ran.nextFloat(rowVertPad)
				rowTable.add(tableTable).padLeft(ran.nextFloat(space) + 5f).padBottom(vertPad).expand().left().bottom()

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
		mainTable.add(navigationBar).growX()
	}

	fun createBountiesTable()
	{
		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_wood_1"))).tint(Color(0.8f, 0.8f, 0.8f, 1f))

		mainTable.clear()

		val gameDataBar = GameDataBar()
		mainTable.add(gameDataBar).growX()
		mainTable.row()
		mainTable.add(Label("Bounties", Global.skin, "title")).expandX().center().padTop(20f)
		mainTable.row()
		mainTable.add(Seperator(Global.skin, false)).growX().pad(5f)
		mainTable.row()

		val bountiesTable = Table()
		bountiesTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.4f))

		val numBounties = Global.data.getAllowedNumBounties()

		// sort to be collected first, then unset, then by remaining duration
		val bounties = Array<AbstractBounty?>()
		for (bounty in Global.data.bounties)
		{
			if (bounty.isComplete())
			{
				bounties.add(bounty)
			}
		}

		if (Global.data.bounties.size < numBounties)
		{
			for (i in 0 until numBounties-Global.data.bounties.size)
			{
				bounties.add(null)
			}
		}

		for (bounty in Global.data.bounties.filter { !it.isComplete() }.sortedBy { it.remaining() })
		{
			bounties.add(bounty)
		}

		var bright = false
		for (bounty in bounties)
		{
			var bounty: AbstractBounty? = bounty
			val bountyTable = Table()

			if (bright)
			{
				bountyTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
			}
			bright = !bright

			fun fillTable()
			{
				bountyTable.clear()

				if (bounty == null)
				{
					val emptyTile = Stack()
					emptyTile.add(SpriteWidget(AssetManager.loadSprite("Icons/Empty"), 64f, 64f))
					emptyTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 64f, 64f))

					bountyTable.add(emptyTile)
					bountyTable.add(Label("Tap to select a bounty", Global.skin)).expandX()
					bountyTable.add(Label("-- : -- : --", Global.skin))

					bountyTable.addClickListener {

						val ran = Random.obtainTS(Global.data.lastRefreshTimeMillis + Global.data.bounties.size + Global.data.completedBounties * 90)

						val greyoutTable = createGreyoutTable(stage, 0.3f, 0.7f)

						val bountyCards = Array<CardWidget>()
						for (i in 0 until 4)
						{
							val newBounty = AbstractBounty.generate(ran)
							val card = CardWidget(newBounty.createCardTable(), newBounty.createCardTable(), AssetManager.loadTextureRegion("white")!!, data = newBounty, border = newBounty.ascension.colour)
							bountyCards.add(card)

							card.setFacing(true, false)
							stage.addActor(card)
						}

						for (card in bountyCards)
						{
							card.canZoom = false
							card.addPick("", {
								val chosen = it.data as AbstractBounty
								Global.data.bounties.add(chosen)
								bounty = chosen
								chosen.startTimeMillis = currentTimeMillis()

								Save.save()

								fillTable()

								greyoutTable.fadeOutAndRemove(0.6f)

								for (card in bountyCards)
								{
									card.remove()

									val table = Table()
									table.isTransform = true
									table.originX = card.referenceWidth / 2
									table.originY = card.referenceHeight / 2
									table.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 30, 30, 30, 30))
									table.setSize(card.referenceWidth, card.referenceHeight)
									table.setScale(card.contentTable.scaleX)
									table.setPosition(card.x + card.contentTable.x, card.y + card.contentTable.y)

									if (card == it)
									{
										val cardDissolve = DissolveEffect(table, 1.5f, AssetManager.loadTextureRegion("GUI/holygradient")!!, 6f)
										cardDissolve.setPosition(card.x, card.y)
										cardDissolve.setSize(card.width, card.height)

										stage.addActor(cardDissolve)
									}
									else
									{
										val cardDissolve = DissolveEffect(table, 1.5f, AssetManager.loadTextureRegion("GUI/burngradient")!!, 1f)
										cardDissolve.setPosition(card.x, card.y)
										cardDissolve.setSize(card.width, card.height)

										stage.addActor(cardDissolve)
									}
								}
							})
						}

						CardWidget.layoutCards(bountyCards, Direction.CENTER, null, true, false, 0.1f)
					}
				}
				else
				{
					bountyTable.add(bounty!!.getTile())
					bountyTable.add(Label(bounty!!.title, Global.skin).wrap().tint(bounty!!.ascension.colour.copy().lerp(Colour.WHITE, 0.5f).color())).growX().padLeft(10f).padRight(5f)

					if (bounty!!.isComplete())
					{
						val takeButton = TextButton("Collect", Global.skin)
						takeButton.addClickListener {
							bounty!!.collect(takeButton, gameDataBar, navigationBar)
							Global.data.bounties.removeValue(bounty!!, true)
							bounty = null
							Global.data.completedBounties++

							Save.save()

							fillTable()
						}

						val widthLabel = Label("-- : -- : --", Global.skin)
						bountyTable.add(takeButton).width(widthLabel.prefWidth)
					}
					else
					{
						bountyTable.add(CountdownWidget("", bounty!!.endTime(), true))
					}
				}
			}

			fillTable()

			bountiesTable.add(bountyTable).growX().pad(3f)
			bountiesTable.row()
		}

		val nextBountyTable = Table()
		if (bright)
		{
			nextBountyTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
		}

		nextBountyTable.add(Label("Next bounty slot at " + Global.data.nextBountyLevel(), Global.skin))

		bountiesTable.add(nextBountyTable).height(64f).growX()
		bountiesTable.row()

		bountiesTable.add(Table()).grow()

		val scroll = ScrollPane(bountiesTable)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setOverscroll(false, false)
		scroll.setForceScroll(false, true)

		mainTable.add(scroll).grow().pad(5f)
		mainTable.row()
		mainTable.add(Seperator(Global.skin, false)).growX().pad(5f)
		mainTable.row()

		val backButton = TextButton("Back", Global.skin)
		backButton.addClickListener {
			createHubTable()
		}

		mainTable.add(backButton).expandX().left().pad(5f)
	}

	override fun doRender(delta: Float)
	{

	}
}