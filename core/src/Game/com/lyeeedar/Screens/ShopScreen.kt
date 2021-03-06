package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.EntityLoader
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.stats
import com.lyeeedar.Game.*
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Systems.directionSprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.actors.alpha
import ktx.actors.then
import ktx.collections.gdxArrayOf

class ShopScreen : AbstractScreen()
{
	val merchant = AssetManager.loadSprite("Oryx/uf_split/uf_heroes/merchant_a", drawActualSize = true)
	val counter = AssetManager.loadSprite("Oryx/Custom/terrain/table", drawActualSize = true)
	val counter_papers = AssetManager.loadSprite("Oryx/Custom/terrain/table_papers", drawActualSize = true)
	val counter_large = AssetManager.loadSprite("Oryx/uf_split/uf_terrain/table", drawActualSize = true)
	val counter_large_sold = AssetManager.loadSprite("Oryx/Custom/terrain/table_large_sold", drawActualSize = true)
	val equipmentChest = AssetManager.loadSprite("Oryx/Custom/items/chest_equipment", drawActualSize = true)
	val heroesChest = AssetManager.loadSprite("Oryx/Custom/items/chest_heroes", drawActualSize = true)

	class ShopWare(val wareTable: Table, val cost: Int, val previewTable: Table, val colour: Colour, val purchaseAction: (Actor)->Unit, val singlePurchase: Boolean)
	val itemsToBuy = Array2D<ShopWare?>(4, 3) { x,y -> null }
	val purchasesTable = Table()
	val navigationBar = NavigationBar(MainGame.ScreenEnum.SHOP)
	val gameDataBar = GameDataBar()

	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.4f, 0.4f, 0.4f, 1f))

		mainTable.add(gameDataBar).growX()
		mainTable.row()

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

		if (!Statics.release)
		{
			debugConsole.register("addgold", "", { args, console ->

				val num = args[0].toInt()
				Global.data.gold += num

				true
			})
		}
	}

	fun createWares()
	{
		Global.data.refresh()

		for (i in 0 until 4)
		{
			val equip = Global.data.currentShopEquipment[i]
			if (equip == null)
			{
				itemsToBuy[i, 0] = null
				continue
			}

			val tileTable = Table()
			tileTable.add(equip.createTile(48f)).size(48f).padBottom(42f)

			val ware = ShopWare(tileTable, (1000 * equip.ascension.multiplier).ciel(), equip.createCardTable(), equip.ascension.colour, {
				Global.data.equipment.add(equip)

				val src = it.localToStageCoordinates(Vector2(24f, 24f))

				val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
				val dst = dstTable.localToStageCoordinates(Vector2())

				val widget = MaskedTexture(equip.fullIcon)
				widget.setSize(48f, 48f)

				val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
				sparkleParticle.color = Color.WHITE.cpy().lerp(equip.ascension.colour.color(), 0.3f)
				sparkleParticle.setSize(dstTable.height, dstTable.height)
				sparkleParticle.setPosition(dstTable.x + dstTable.width*0.5f - dstTable.height * 0.5f, dstTable.y)

				val sequence =
					parallel(
						mote(src, dst, 1f, Interpolation.exp5, false),
						scaleTo(0.7f, 0.7f, 1f),
					delay(0.9f) then lambda { stage.addActor(sparkleParticle) } then fadeOut(0.1f)) then
						removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)
			}, true)
			itemsToBuy[i, 0] = ware
		}

		for (i in 0 until 4)
		{
			val hero = Global.data.currentShopHeroes[i]
			val heroData = Global.data.heroPool.firstOrNull { it.factionEntity.entityPath == hero }

			if (heroData == null)
			{
				itemsToBuy[i, 1] = null
				continue
			}

			val ascension = heroData.factionEntity.rarity.ascension

			val tileTable = Table()

			tileTable.add(heroData.factionEntity.createTile(48f, ascension)).size(48f).padBottom(42f)

			val detailsTable = heroData.factionEntity.createCardTable(ascension, false)

			val ware = ShopWare(tileTable, (1000 * ascension.multiplier).ciel(), detailsTable, heroData.factionEntity.rarity.colour, {

				heroData.ascensionShards += 1

				val src = it.localToStageCoordinates(Vector2(24f, 24f))

				val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
				val dst = dstTable.localToStageCoordinates(Vector2())

				val widget = SpriteWidget(AssetManager.loadSprite("Particle/shard"), 16f, 16f)
				widget.color = Colour.WHITE.copy().lerp(heroData.factionEntity.rarity.colour, 0.7f).color()
				widget.setSize(16f, 16f)

				val chosenDst = dst.cpy()
				chosenDst.x += dstTable.width * 0.5f * Random.random()
				chosenDst.y += dstTable.height * 0.5f * Random.random()

				val particle = AssetManager.loadParticleEffect("GetShard", timeMultiplier = 1.2f)
				particle.colour = Colour.WHITE.copy().lerp(heroData.factionEntity.rarity.colour, 0.7f)

				val sparkleParticle = ParticleEffectActor(particle.getParticleEffect(), true)
				sparkleParticle.setSize(dstTable.height, dstTable.height)
				sparkleParticle.setPosition(chosenDst.x - 16f, chosenDst.y - 16f)

				val sequence =
					parallel(
						mote(src, chosenDst, 1f + i*0.02f, Interpolation.exp5, true),
						scaleTo(0.7f, 0.7f, 1f),
						delay(0.8f + i*0.02f) then lambda { stage.addActor(sparkleParticle) } then fadeOut(0.1f)) then
						removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)

			}, true)
			itemsToBuy[i, 1] = ware
		}

		// 9 equipment
		val ranEquipTile = Table()
		ranEquipTile.add(SpriteWidget(equipmentChest, 48f, 48f))

		val ranEquipFocus = Table()
		ranEquipFocus.add(Label("Equipment Chest", Statics.skin, "cardtitle").wrap().align(Align.center)).growX().center()
		ranEquipFocus.row()
		ranEquipFocus.add(Label("A chest filled with 9 random pieces of equipment.", Statics.skin, "card").wrap()).growX().pad(20f)

		val ranEquip = ShopWare(ranEquipTile, 3000, ranEquipFocus, Ascension.EXTRAORDINARY.colour, {
			val cards = Array<CardWidget>()

			for (i in 0 until 9)
			{
				val equip = EquipmentCreator.createRandom(Global.data.getCurrentLevel())
				val card = CardWidget(equip.createCardTable(), equip.createCardTable(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, border = equip.ascension.colour)
				card.canZoom = false
				card.addPick("", {

					Global.data.equipment.add(equip)

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val widget = MaskedTexture(equip.fullIcon)
					widget.setSize(48f, 48f)
					widget.setPosition(src.x, src.y)
					widget.toFront()

					val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
					sparkleParticle.color = Color.WHITE.cpy().lerp(equip.ascension.colour.color(), 0.3f)
					sparkleParticle.setSize(dstTable.height, dstTable.height)
					sparkleParticle.setPosition(dstTable.x + dstTable.width*0.5f - dstTable.height * 0.5f, dstTable.y)

					val sequence =
						parallel(
							mote(src, dst, 1f, Interpolation.exp5, false),
							scaleTo(0.7f, 0.7f, 1f),
							delay(0.9f) then lambda { stage.addActor(sparkleParticle) } then fadeOut(0.1f)) then
							removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				})
				cards.add(card)

				if (equip.ascension != Ascension.MUNDANE)
				{
					val highestAscension = Ascension.FABLED
					val alpha = equip.ascension.ordinal.toFloat() / highestAscension.ordinal.toFloat()

					card.flipEffect = ParticleEffectActor(AssetManager.loadParticleEffect("FlipCard", colour = equip.ascension.colour.copy().a(0.5f + 0.5f * alpha), scale = (0.8f + 0.2f * alpha), timeMultiplier = (1f / (2f * alpha))).getParticleEffect(), true)
					card.flipDelay = 2f * alpha
				}
			}

			displayLoot(cards)
		}, false)
		itemsToBuy[0, 2] = ranEquip

		// 9 equipment of weight
		val weightChosen = Global.data.currentShopWeight
		val ranEquipWeightTile = Table()
		val ranEquipWeightStack = Stack()
		ranEquipWeightTile.add(ranEquipWeightStack).grow()

		ranEquipWeightStack.add(SpriteWidget(equipmentChest, 48f, 48f))
		ranEquipWeightStack.addTable(SpriteWidget(weightChosen.icon, 24f, 24f)).size(24f).padBottom(16f)

		val ranEquipWeightFocus = Table()
		ranEquipWeightFocus.add(Label(weightChosen.niceName + " Equipment Chest", Statics.skin, "cardtitle").wrap().align(Align.center)).growX().center()
		ranEquipWeightFocus.row()
		ranEquipWeightFocus.add(Label("A chest filled with 9 random pieces of ${weightChosen.niceName} equipment.", Statics.skin, "card").wrap()).growX().pad(20f)

		val ranEquipWeight = ShopWare(ranEquipWeightTile, 4000, ranEquipWeightFocus, Ascension.LEGENDARY.colour, {
			val cards = Array<CardWidget>()

			for (i in 0 until 9)
			{
				val equip = EquipmentCreator.createRandom(Global.data.getCurrentLevel(), weight = weightChosen)
				val card = CardWidget(equip.createCardTable(), equip.createCardTable(), AssetManager.loadTextureRegion("GUI/EquipmentCardback")!!, border = equip.ascension.colour)
				card.canZoom = false
				card.addPick("", {

					Global.data.equipment.add(equip)

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val widget = MaskedTexture(equip.fullIcon)
					widget.setSize(48f, 48f)
					widget.setPosition(src.x, src.y)
					widget.toFront()

					val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
					sparkleParticle.color = Color.WHITE.cpy().lerp(equip.ascension.colour.color(), 0.3f)
					sparkleParticle.setSize(dstTable.height, dstTable.height)
					sparkleParticle.setPosition(dstTable.x + dstTable.width*0.5f - dstTable.height * 0.5f, dstTable.y)

					val sequence =
						parallel(
							mote(src, dst, 1f, Interpolation.exp5, false),
							scaleTo(0.7f, 0.7f, 1f),
							delay(0.9f) then lambda { stage.addActor(sparkleParticle) } then fadeOut(0.1f)) then
							removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				})
				cards.add(card)

				if (equip.ascension != Ascension.MUNDANE)
				{
					val highestAscension = Ascension.FABLED
					val alpha = equip.ascension.ordinal.toFloat() / highestAscension.ordinal.toFloat()

					card.flipEffect = ParticleEffectActor(AssetManager.loadParticleEffect("FlipCard", colour = equip.ascension.colour.copy().a(0.5f + 0.5f * alpha), scale = (0.8f + 0.2f * alpha), timeMultiplier = (1f / (2f * alpha))).getParticleEffect(), true)
					card.flipDelay = 2f * alpha
				}
			}

			displayLoot(cards)
		}, false)
		itemsToBuy[1, 2] = ranEquipWeight

		// 9 heroes
		val ranHeroTile = Table()
		ranHeroTile.add(SpriteWidget(heroesChest, 48f, 48f))

		val ranHeroFocus = Table()
		ranHeroFocus.add(Label("Hero Chest", Statics.skin, "cardtitle").wrap().align(Align.center)).growX().center()
		ranHeroFocus.row()
		ranHeroFocus.add(Label("A chest filled with 9 random heroes from any of your unlocked factions.", Statics.skin, "card").wrap()).growX().pad(20f)

		val ranHero = ShopWare(ranHeroTile, 3000, ranHeroFocus, Ascension.EXTRAORDINARY.colour, {
			dropHeroes(Global.data.unlockedFactions)
		}, false)
		itemsToBuy[2, 2] = ranHero

		// 9 heroes from faction
		val faction = Global.data.currentShopFaction
		val ranHeroFactionTile = Table()
		val ranHeroFactionStack = Stack()
		ranHeroFactionTile.add(ranHeroFactionStack).grow()

		ranHeroFactionStack.add(SpriteWidget(heroesChest, 48f, 48f))
		ranHeroFactionStack.addTable(SpriteWidget(faction.icon, 24f, 24f)).size(24f).padBottom(16f)

		val ranHeroFactionFocus = Table()
		ranHeroFactionFocus.add(Label(faction.name + " Hero Chest", Statics.skin, "cardtitle").wrap().align(Align.center)).growX().center()
		ranHeroFactionFocus.row()
		ranHeroFactionFocus.add(Label("A chest filled with 9 random heroes from the ${faction.name} faction.", Statics.skin, "card").wrap()).growX().pad(20f)

		val ranHeroWeight = ShopWare(ranHeroFactionTile, 4000, ranHeroFactionFocus, Ascension.LEGENDARY.colour, {
			dropHeroes(gdxArrayOf(faction))
		}, false)
		itemsToBuy[3, 2] = ranHeroWeight
	}

	fun dropHeroes(factions: Array<Faction>)
	{
		val cards = Array<CardWidget>()

		var unlockedHero = false
		for (i in 0 until 9)
		{
			val possibleDrops = Array<FactionEntity>()
			for (faction in factions)
			{
				for (hero in faction.heroes)
				{
					val extraWeight = if (Global.data.heroPool.any{ it.factionEntity == hero}) 2 else 1
					val heroData = Global.data.heroPool.firstOrNull { it.factionEntity == hero }

					if (heroData == null && hero.rarity.ordinal > Rarity.COMMON.ordinal && unlockedHero)
					{
						continue // only drop 1 new hero at a time
					}

					for (i in 0 until hero.rarity.dropRate * extraWeight)
					{
						possibleDrops.add(hero)
					}
				}
			}

			val hero = possibleDrops.random()
			var heroData = Global.data.heroPool.firstOrNull { it.factionEntity == hero }
			var isNewHero = false

			if (heroData == null)
			{
				heroData = EntityData(hero, Ascension.MUNDANE, 1)
				Global.data.heroPool.add(heroData)
				isNewHero = true
				unlockedHero = true
			}
			val ascension = heroData.factionEntity.rarity.ascension

			val card = CardWidget(hero.createCardTable(ascension, isNewHero), hero.createCardTable(ascension, isNewHero), AssetManager.loadTextureRegion("GUI/CharacterCardback")!!, border = hero.rarity.colour)
			card.canZoom = false
			card.addPick("") {

				if (isNewHero)
				{
					card.flipEffect?.remove()

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val entity = EntityLoader.load(hero.entityPath, Global.resolveInstant)
					//entity.stats().ascension = ascension
					entity.stats().level = 1
					Global.engine.directionSprite().processEntity(entity, 0f)
					val sprite = Sprite((entity.renderable().renderable as Sprite).textures[0])

					val widget = SpriteWidget(sprite, 48f, 48f)
					widget.setSize(48f, 48f)
					widget.setPosition(src.x, src.y)
					widget.toFront()

					val chosenDst = dst.cpy()
					chosenDst.x += dstTable.width * 0.5f * Random.random()
					chosenDst.y += dstTable.height * 0.5f * Random.random()

					val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment").getParticleEffect(), true)
					sparkleParticle.setSize(dstTable.height, dstTable.height)
					sparkleParticle.setPosition(chosenDst.x - 16f, chosenDst.y - 16f)

					val sequence =
						parallel(
							mote(src, chosenDst, 1f + i * 0.02f, Interpolation.exp5, false),
							scaleTo(0.7f, 0.7f, 1f),
							delay(0.8f + i * 0.02f) then lambda { stage.addActor(sparkleParticle) } then fadeOut(0.1f)) then
							removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				}
				else
				{
					heroData.ascensionShards += 1

					val src = it.localToStageCoordinates(Vector2(24f, 24f))

					val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
					val dst = dstTable.localToStageCoordinates(Vector2())

					val widget = SpriteWidget(AssetManager.loadSprite("Particle/shard"), 16f, 16f)
					widget.color = Colour.WHITE.copy().lerp(hero.rarity.colour, 0.7f).color()
					widget.setSize(16f, 16f)
					widget.setPosition(src.x, src.y)
					widget.toFront()

					val chosenDst = dst.cpy()
					chosenDst.x += dstTable.width * 0.5f * Random.random()
					chosenDst.y += dstTable.height * 0.5f * Random.random()

					val particle = AssetManager.loadParticleEffect("GetShard", timeMultiplier = 1.2f)
					particle.colour = Colour.WHITE.copy().lerp(hero.rarity.colour, 0.7f)

					val shardParticle = ParticleEffectActor(particle.getParticleEffect(), true)
					shardParticle.setSize(dstTable.height, dstTable.height)
					shardParticle.setPosition(chosenDst.x - 16f, chosenDst.y - 16f)

					val sequence =
						parallel(
							mote(src, chosenDst, 1f + i * 0.02f, Interpolation.exp5, true),
							scaleTo(0.7f, 0.7f, 1f),
							delay(0.8f + i * 0.02f) then lambda { stage.addActor(shardParticle) } then fadeOut(0.1f)) then
							removeActor()
					widget.addAction(sequence)

					stage.addActor(widget)
				}

				Save.save()
			}
			cards.add(card)

			if (isNewHero)
			{
				val particle = AssetManager.loadParticleEffect("NewHero")
				particle.colour = Colour.WHITE.copy().lerp(hero.rarity.colour, 0.7f)

				card.flipEffect = ParticleEffectActor(particle.getParticleEffect(), false)
				card.flipDelay = 1f
			}
			else if (hero.rarity.ordinal >= Rarity.RARE.ordinal)
			{
				val alpha = 0.5f
				card.flipEffect = ParticleEffectActor(AssetManager.loadParticleEffect("FlipCard", colour = hero.rarity.colour.copy().a(0.5f + 0.5f * alpha), scale = (0.8f + 0.2f * alpha), timeMultiplier = (1f / (2f * alpha))).getParticleEffect(), true)
				card.flipDelay = 2f * alpha
			}
		}

		displayLoot(cards)
	}

	fun displayLoot(cards: Array<CardWidget>)
	{
		val greyoutTable = createGreyoutTable(stage)

		val cardsTable = Table()
		greyoutTable.add(cardsTable).grow()
		greyoutTable.row()

		val buttonTable = Table()
		greyoutTable.add(buttonTable).growX()

		val flipAllButton = TextButton("Flip All", Statics.skin)
		flipAllButton.addClickListener {

			var i = 0
			for (card in cards)
			{
				if (!card.faceup)
				{
					Future.call({ card.flip(true) }, i * 0.05f)
					i++
				}
			}
		}
		buttonTable.add(flipAllButton).pad(10f).expandX().right()
		flipAllButton.alpha = 0f

		var flippedCards = 0
		for (card in cards)
		{
			card.flipFun = {
				flippedCards++
				if (flippedCards == cards.size)
				{
					buttonTable.clear()

					val takeAllButton = TextButton("Take All", Statics.skin)
					takeAllButton.addClickListener {

						var i = 0
						for (card in cards)
						{
							if (!card.isPicked)
							{
								Future.call({ card.pickFuns[0].pickFun(card) }, i * 0.05f)
								i++
							}
							card.isPicked = true
						}
					}
					buttonTable.add(takeAllButton).pad(10f).expandX().right()
				}
			}
		}

		var gatheredLoot = 0
		for (card in cards)
		{
			val oldPick = card.pickFuns[0]
			card.pickFuns.clear()
			card.addPick("", {
				card.isPicked = true
				oldPick.pickFun(card)

				card.remove()

				gatheredLoot++
				if (gatheredLoot == cards.size)
				{
					greyoutTable.fadeOutAndRemove(0.6f)

					Save.save()
				}

				val table = Table()
				table.isTransform = true
				table.originX = card.referenceWidth / 2
				table.originY = card.referenceHeight / 2
				table.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 30, 30, 30, 30))
				table.setSize(card.referenceWidth, card.referenceHeight)
				table.setScale(card.contentTable.scaleX)
				table.setPosition(card.x + card.contentTable.x, card.y + card.contentTable.y)

				val cardDissolve = DissolveEffect(table, 1.5f, AssetManager.loadTextureRegion("GUI/holygradient")!!, 6f)
				cardDissolve.setPosition(card.x, card.y)
				cardDissolve.setSize(card.width, card.height)

				stage.addActor(cardDissolve)
			})
		}

		val chestClosed = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold"), 128f, 128f)
		val chestOpen = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold_open"), 128f, 128f)

		chestClosed.setPosition(stage.width / 2f - 64f, stage.height / 2f - 64f)
		chestClosed.setSize(128f, 128f)
		chestClosed.addAction(WobbleAction(0f, 35f, 0.1f, 2f))
		stage.addActor(chestClosed)

		Future.call(
			{
				chestClosed.remove()

				chestOpen.setPosition(stage.width / 2f - 64f, stage.height / 2f - 64f)
				chestOpen.setSize(128f, 128f)
				stage.addActor(chestOpen)

				val effect = AssetManager.loadParticleEffect("ChestOpen").getParticleEffect()
				val particleActor = ParticleEffectActor(effect, true)
				particleActor.setSize(128f, 128f)
				particleActor.setPosition(stage.width / 2f - 64f, stage.height / 2f - 64f)
				stage.addActor(particleActor)

				Future.call(
					{
						val sequence = delay(0.2f) then fadeOut(0.3f) then removeActor()
						chestOpen.addAction(sequence)

						for (card in cards)
						{
							stage.addActor(card)
						}

						CardWidget.layoutCards(cards, Direction.CENTER, cardsTable, startScale = 0.3f)
						flipAllButton.alpha = 1f
					}, 0.4f)
			}, 2f)
	}

	fun fillPurchasesTable()
	{
		purchasesTable.clear()

		purchasesTable.add(CountdownWidget("New goods in", Global.data.lastRefreshTimeMillis + Global.data.refreshTimeMillis)).colspan(4)
		purchasesTable.row()

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

					val costTable = Table()
					costTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))

					val costLabel = Label(ware.cost.prettyPrint(), Statics.skin)
					if (ware.cost > Global.data.gold)
					{
						costLabel.setColor(0.85f, 0f, 0f, 1f)
					}

					costTable.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 16f, 16f))
					costTable.add(costLabel)

					purchaseStack.addTable(costTable).expand().bottom().padBottom(24f)

					purchaseStack.addClickListener {
						val card = CardWidget(ware.previewTable, ware.previewTable, AssetManager.loadTextureRegion("GUI/MoneyCardback")!!, border = ware.colour)
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
									if (y == 0)
									{
										Global.data.currentShopEquipment[x] = null
									}
									else if (y == 1)
									{
										Global.data.currentShopHeroes[x] = null
									}

									itemsToBuy[x, y] = null
								}

								card.remove()

								fillPurchasesTable()

								Save.save()
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

	override fun show()
	{
		super.show()

		createWares()
		fillPurchasesTable()
		gameDataBar.rebind()

		Global.data.lastViewedShopWares = Global.data.lastRefreshTime

		Save.save()
	}

	override fun doRender(delta: Float)
	{

	}
}