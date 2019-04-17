package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Game.EquipmentCreator
import com.lyeeedar.Game.FactionEntity
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.actors.alpha
import ktx.actors.then

class ShopScreen : AbstractScreen()
{
	val merchant = AssetManager.loadSprite("Oryx/uf_split/uf_heroes/merchant_a", drawActualSize = true)
	val counter = AssetManager.loadSprite("Oryx/Custom/terrain/table", drawActualSize = true)
	val counter_papers = AssetManager.loadSprite("Oryx/Custom/terrain/table_papers", drawActualSize = true)
	val counter_large = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/table", drawActualSize = true)
	val counter_large_sold = AssetManager.loadSprite("Oryx/Custom/terrain/table_large_sold", drawActualSize = true)
	val equipmentChest = AssetManager.loadSprite("Oryx/Custom/items/chest_equipment", drawActualSize = true)
	val heroesChest = AssetManager.loadSprite("Oryx/Custom/items/chest_heroes", drawActualSize = true)

	class ShopWare(val wareTable: Table, val cost: Int, val previewTable: Table, val purchaseAction: (Actor)->Unit, val singlePurchase: Boolean)
	val itemsToBuy = Array2D<ShopWare?>(4, 3) { x,y -> null }
	val purchasesTable = Table()
	val navigationBar = NavigationBar(MainGame.ScreenEnum.SHOP)

	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.4f, 0.4f, 0.4f, 1f))

		val shopTable = Table()

		val merchantTable = Table()
		val wallTable = Table()
		wallTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/wall_stone_14")))

		val merchantRow = Table()
		merchantRow.add(SpriteWidget(merchant, 48f, 48f)).size(48f).expandX().center()
		val counterRow = Table()
		for (i in 0 until 4)
		{
			if (i == 1)
			{
				counterRow.add(SpriteWidget(counter_papers, 48f, 48f)).size(48f)
			}
			else
			{
				counterRow.add(SpriteWidget(counter, 48f, 48f)).size(48f)
			}
		}

		merchantTable.add(wallTable).height(48f).growX()
		merchantTable.row()
		merchantTable.add(merchantRow).growX()
		merchantTable.row()
		merchantTable.add(counterRow).growX()

		shopTable.add(merchantTable).growX().top()
		shopTable.row()

		createWares()
		fillPurchasesTable()

		shopTable.add(purchasesTable).grow().padTop(20f).padBottom(20f)

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(navigationBar).growX()
	}

	fun createWares()
	{
		for (i in 0 until 4)
		{
			val equip = EquipmentCreator.createRandom(10)
			val tileTable = Table()
			tileTable.add(equip.createTile(48f)).size(48f).padBottom(42f)

			val ware = ShopWare(tileTable, (1000 * equip.ascension.multiplier).ciel(), equip.createCardTable(), {
				Global.data.equipment.add(equip)

				val src = it.localToStageCoordinates(Vector2(24f, 24f))

				val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
				val dst = dstTable.localToStageCoordinates(Vector2())

				val widget = MaskedTexture(equip.fullIcon)
				widget.setSize(48f, 48f)
				val sequence = mote(src, dst, 1f, Interpolation.exp5, false) then removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)
			}, true)
			itemsToBuy[i, 0] = ware
		}

		for (i in 0 until 4)
		{
			val possibleDrops = Array<FactionEntity>()
			for (faction in Global.data.unlockedFactions)
			{
				for (hero in faction.heroes)
				{
					if (Global.data.heroPool.any{ it.factionEntity == hero})
					{
						for (i in 0 until hero.rarity.dropRate)
						{
							possibleDrops.add(hero)
						}
					}
				}
			}

			val hero = possibleDrops.random()
			val heroData = Global.data.heroPool.first { it.factionEntity == hero }

			var ascension = Ascension.getWeightedAscension()
			if (ascension.ordinal >= heroData.ascension.ordinal)
			{
				val newOrdinal = max(0, heroData.ascension.ordinal-1)
				ascension = Ascension.Values[newOrdinal]
			}

			val tileTable = Table()

			tileTable.add(hero.createTile(48f, ascension)).size(48f).padBottom(42f)

			val detailsTable = hero.createCardTable(ascension)

			val ware = ShopWare(tileTable, (1000 * ascension.multiplier).ciel(), detailsTable, {

				val droppedShards = max(1, ascension.shardsRequired / 3)
				heroData.ascensionShards += droppedShards

				val src = it.localToStageCoordinates(Vector2(24f, 24f))

				val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
				val dst = dstTable.localToStageCoordinates(Vector2())

				for (i in 0 until droppedShards)
				{
					val widget = SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/shard"), 48f, 48f)
					widget.setSize(48f, 48f)
					val sequence = mote(src, dst, 1f, Interpolation.exp5, true) then removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				}

			}, true)
			itemsToBuy[i, 1] = ware
		}

		// 9 equipment
		val ranEquipTile = Table()
		ranEquipTile.add(SpriteWidget(equipmentChest, 48f, 48f))

		val ranEquipFocus = Table()

		val ranEquip = ShopWare(ranEquipTile, 3000, ranEquipFocus, {
			val cards = Array<CardWidget>()

			for (i in 0 until 9)
			{
				val equip = EquipmentCreator.createRandom(1)
				val card = CardWidget(equip.createCardTable(), equip.createCardTable(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!)
				card.canZoom = false
				card.addPick("", {

					Global.data.equipment.add(equip)

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val widget = MaskedTexture(equip.fullIcon)
					widget.setSize(48f, 48f)
					val sequence = mote(src, dst, 1f, Interpolation.exp5, false) then removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				})
				cards.add(card)
			}

			displayLoot(cards)
		}, false)
		itemsToBuy[0, 2] = ranEquip

		// 9 equipment of weight
		val weightChosen = EquipmentWeight.Values.random()
		val ranEquipWeightTile = Table()
		val ranEquipWeightStack = Stack()
		ranEquipWeightTile.add(ranEquipWeightStack).grow()

		ranEquipWeightStack.add(SpriteWidget(equipmentChest, 48f, 48f))
		ranEquipWeightStack.addTable(SpriteWidget(weightChosen.icon, 24f, 24f)).size(24f).padBottom(16f)

		val ranEquipWeightFocus = Table()

		val ranEquipWeight = ShopWare(ranEquipWeightTile, 4000, ranEquipWeightFocus, {
			val cards = Array<CardWidget>()

			for (i in 0 until 9)
			{
				val equip = EquipmentCreator.createRandom(1, weight = weightChosen)
				val card = CardWidget(equip.createCardTable(), equip.createCardTable(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!)
				card.canZoom = false
				card.addPick("", {

					Global.data.equipment.add(equip)

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val widget = MaskedTexture(equip.fullIcon)
					widget.setSize(48f, 48f)
					val sequence = mote(src, dst, 1f, Interpolation.exp5, false) then removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				})
				cards.add(card)
			}

			displayLoot(cards)
		}, false)
		itemsToBuy[1, 2] = ranEquipWeight

		// 9 heroes
		val ranHeroTile = Table()
		ranHeroTile.add(SpriteWidget(heroesChest, 48f, 48f))

		val ranHeroFocus = Table()

		val ranHero = ShopWare(ranHeroTile, 3000, ranHeroFocus, {

		}, false)
		itemsToBuy[2, 2] = ranHero

		// 9 heroes from faction
		val faction = Global.data.unlockedFactions.random()
		val ranHeroWeightTile = Table()
		val ranHeroWeightStack = Stack()
		ranHeroWeightTile.add(ranHeroWeightStack).grow()

		ranHeroWeightStack.add(SpriteWidget(heroesChest, 48f, 48f))
		ranHeroWeightStack.addTable(SpriteWidget(faction.icon, 24f, 24f)).size(24f).padBottom(16f)

		val ranHeroWeightFocus = Table()

		val ranHeroWeight = ShopWare(ranHeroWeightTile, 4000, ranHeroWeightFocus, {

		}, false)
		itemsToBuy[3, 2] = ranHeroWeight
	}

	fun displayLoot(cards: Array<CardWidget>)
	{
		val greyoutTable = Table()
		greyoutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.9f))
		greyoutTable.touchable = Touchable.enabled
		greyoutTable.setFillParent(true)

		stage.addActor(greyoutTable)

		val cardsTable = Table()
		greyoutTable.add(cardsTable).grow()
		greyoutTable.row()

		val buttonTable = Table()
		greyoutTable.add(buttonTable).growX()

		val flipAllButton = TextButton("Flip All", Global.skin)
		flipAllButton.addClickListener {
			for (card in cards)
			{
				card.flip(true)
			}

			buttonTable.clear()

			val takeAllButton = TextButton("Take All", Global.skin)
			takeAllButton.addClickListener {
				for (card in cards)
				{
					card.pickFuns[0].pickFun(card)
				}
			}
			buttonTable.add(takeAllButton).pad(10f).expandX().right()
		}
		buttonTable.add(flipAllButton).pad(10f).expandX().right()
		flipAllButton.alpha = 0f

		var gatheredLoot = 0
		for (card in cards)
		{
			val oldPick = card.pickFuns[0]
			card.pickFuns.clear()
			card.addPick("", {
				oldPick.pickFun(card)

				card.remove()

				gatheredLoot++
				if (gatheredLoot == cards.size)
				{
					greyoutTable.remove()
				}
			})

			stage.addActor(card)
		}

		val chestClosed = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold"), 128f, 128f)
		val chestOpen = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold_open"), 128f, 128f)

		chestClosed.setPosition(stage.width / 2f - 64f, stage.height / 2f - 64f)
		chestClosed.setSize(128f, 128f)
		chestClosed.addAction(WobbleAction(0f, 25f, 0.1f, 1.5f))
		stage.addActor(chestClosed)

		Future.call(
			{
				chestClosed.remove()

				chestOpen.setPosition(stage.width / 2f - 64f, stage.height / 2f - 64f)
				chestOpen.setSize(128f, 128f)
				stage.addActor(chestOpen)

				Future.call(
					{
						val sequence = delay(0.2f) then fadeOut(0.3f) then removeActor()
						chestOpen.addAction(sequence)

						CardWidget.layoutCards(cards, Direction.CENTER, cardsTable)
						flipAllButton.alpha = 1f
					}, 0.2f)
			}, 1.5f)
	}

	fun fillPurchasesTable()
	{
		purchasesTable.clear()

		for (y in 0 until itemsToBuy.height)
		{
			for (x in 0 until itemsToBuy.width)
			{
				val purchaseStack = Stack()

				val ware = itemsToBuy[x, y]
				if (ware == null)
				{
					purchaseStack.addTable(SpriteWidget(counter_large_sold, 48f, 48f)).size(48f).expand().bottom()
					purchaseStack.addTable(Table()).size(48f).padBottom(18f).expand().bottom()
				}
				else
				{
					purchaseStack.addTable(SpriteWidget(counter_large, 48f, 48f)).size(48f).expand().bottom()
					purchaseStack.addTable(ware.wareTable).size(48f).padBottom(18f).expand().bottom()

					val costLabel = Label(ware.cost.prettyPrint(), Global.skin)
					if (ware.cost > Global.data.gold)
					{
						costLabel.setColor(0.85f, 0f, 0f, 1f)
					}
					purchaseStack.addTable(costLabel).expand().bottom().padBottom(24f)

					purchaseStack.addClickListener {
						val card = CardWidget(ware.previewTable, ware.previewTable, AssetManager.loadTextureRegion("GUI/MoneyCardback")!!)
						card.setFacing(true, false)
						card.setPosition(purchaseStack.x + purchaseStack.width / 2f, purchaseStack.y + purchaseStack.height)
						card.setSize(48f, 48f)

						if (Global.data.gold >= ware.cost)
						{
							card.addPick("Buy (" + ware.cost.prettyPrint() + " gold)", {

								Global.data.gold -= ware.cost
								ware.purchaseAction(purchaseStack)

								if (ware.singlePurchase)
								{
									itemsToBuy[x, y] = null
								}

								card.remove()

								fillPurchasesTable()
							})
						}

						card.collapseFun = {
							card.remove()
						}

						stage.addActor(card)

						card.focus()
					}
				}

				purchasesTable.add(purchaseStack).expand()
			}

			purchasesTable.row()
		}
	}

	override fun doRender(delta: Float)
	{

	}
}