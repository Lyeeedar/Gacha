package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Game.EquipmentCreator
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.*
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.ciel
import com.lyeeedar.Util.prettyPrint

class ShopScreen : AbstractScreen()
{
	val merchant = AssetManager.loadSprite("Oryx/uf_split/uf_heroes/merchant_a", drawActualSize = true)
	val counter = AssetManager.loadSprite("Oryx/Custom/terrain/table", drawActualSize = true)
	val counter_papers = AssetManager.loadSprite("Oryx/Custom/terrain/table_papers", drawActualSize = true)
	val counter_large = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/table", drawActualSize = true)
	val counter_large_sold = AssetManager.loadSprite("Oryx/Custom/terrain/table_large_sold", drawActualSize = true)

	class ShopWare(val wareTable: Table, val cost: Int, val previewTable: Table, val purchaseAction: ()->Unit, val singlePurchase: Boolean)
	val itemsToBuy = Array2D<ShopWare?>(4, 3) { x,y -> null }
	val purchasesTable = Table()

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

		shopTable.add(purchasesTable).grow().pad(20f)

		mainTable.add(shopTable).grow()
		mainTable.row()
		mainTable.add(NavigationBar(MainGame.ScreenEnum.SHOP)).growX()
	}

	fun createWares()
	{
		for (i in 0 until 4)
		{
			val equip = EquipmentCreator.createRandom(10)
			val ware = ShopWare(equip.createTile(48f), (1000 * equip.ascension.multiplier).ciel(), equip.createCardTable(), {}, true)
			itemsToBuy[i, 0] = ware
		}
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
					purchaseStack.addTable(Table()).size(48f).padBottom(32f)
				}
				else
				{
					purchaseStack.addTable(SpriteWidget(counter_large, 48f, 48f)).size(48f).expand().bottom()
					purchaseStack.addTable(ware.wareTable).size(48f).padBottom(32f)
					purchaseStack.addTable(Label(ware.cost.prettyPrint(), Global.skin)).expand().bottom().padBottom(24f)

					purchaseStack.addClickListener {
						val card = CardWidget(ware.previewTable, ware.previewTable, AssetManager.loadTextureRegion("GUI/MoneyCardback")!!)
						card.setFacing(true, false)
						card.setPosition(purchaseStack.x, purchaseStack.y)
						card.setSize(48f, 48f)

						if (Global.data.gold >= ware.cost)
						{
							card.addPick("Buy (" + ware.cost.prettyPrint() + " gold)", {

								Global.data.gold -= ware.cost
								ware.purchaseAction()

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

				purchasesTable.add(purchaseStack).expand().pad(5f)
			}

			purchasesTable.row()
		}
	}

	override fun doRender(delta: Float)
	{

	}
}