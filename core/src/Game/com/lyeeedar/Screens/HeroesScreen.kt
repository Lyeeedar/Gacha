package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.*
import com.lyeeedar.Components.*
import com.lyeeedar.Game.EntityData
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Faction
import com.lyeeedar.Game.Save
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.actors.then
import ktx.collections.gdxArrayOf

class HeroesScreen : AbstractScreen()
{
	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.4f, 0.4f, 0.4f, 1f))

		recreateFactionsTable()
		recreateHeroesTable()
		createHeroesTable()

		if (!Statics.release)
		{
			debugConsole.register("addhero", "", fun(args, console): Boolean
			{

				if (args.size != 1)
				{
					console.error("must provide a hero name!")
					return false
				}

				for (faction in Global.data.unlockedFactions)
				{
					for (hero in faction.heroes)
					{
						val entity = EntityLoader.load(hero.entityPath, Global.resolveInstant)
						if (entity.name().name.toLowerCase() == args[0].toLowerCase())
						{
							Global.data.heroPool.add(EntityData(hero, Ascension.MUNDANE, 1))

							return true
						}
					}
				}

				return false
			})

			debugConsole.register("addexp", "", { args, console ->

				val amount = args[0].toInt()
				Global.data.experience += amount

				true
			})
		}
	}

	override fun show()
	{
		super.show()

		recreateFactionsTable()
		recreateHeroesTable()
		createHeroesTable()
	}

	val heroesContentTable = Table()
	fun recreateHeroesTable()
	{
		val contentTable = heroesContentTable
		contentTable.clear()

		contentTable.add(GameDataBar()).growX()
		contentTable.row()

		val heroesButton = Table()
		heroesButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanelHighlight"))
		heroesButton.add(Label("Heroes", Statics.skin)).center()

		val factionsButton = Table()
		factionsButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel"))
		factionsButton.add(Label("Factions", Statics.skin)).center()
		factionsButton.addClickListener {
			createFactionsTable()
		}

		val navigationTable = Table()
		navigationTable.add(heroesButton).growY().width(Value.percentWidth(0.5f, navigationTable))
		navigationTable.add(factionsButton).growY().width(Value.percentWidth(0.5f, navigationTable))

		contentTable.add(navigationTable).growX().height(35f)
		contentTable.row()

		// fill in the content
		class EntityAndData(val data: EntityData, val entity: Entity)
		val allHeroes = com.badlogic.gdx.utils.Array<EntityAndData>()
		for (heroData in Global.data.heroPool)
		{
			val hero = heroData.getEntity("1")
			allHeroes.add(EntityAndData(heroData, hero))
		}

		val heroesTable = Table()

		val heroesARow = 5
		var x = 0
		for (hero in allHeroes.sortedByDescending { it.entity.stats().calculatePowerRating(it.entity) })
		{
			val heroWidget = HeroSelectionWidget(hero.entity, hero.data, true)
			heroWidget.addClickListener {
				createHeroTable(hero.entity, hero.data, false)
			}

			heroesTable.add(heroWidget).size((Statics.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
			x++
			if (x == heroesARow)
			{
				x = 0
				heroesTable.row()
			}
		}

		heroesTable.row()
		heroesTable.add(Table()).colspan(heroesARow).grow()

		val scroll = ScrollPane(heroesTable)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setOverscroll(false, false)
		scroll.setForceScroll(false, true)

		contentTable.add(scroll).grow()
		contentTable.row()
		contentTable.add(NavigationBar(MainGame.ScreenEnum.HEROES)).growX()
	}

	fun createHeroesTable()
	{
		mainTable.clear()
		mainTable.add(heroesContentTable).grow()
	}

	fun createHeroTable(entity: Entity, entityData: EntityData, cameFromFaction: Boolean)
	{
		if (!Statics.release)
		{
			debugConsole.unregister("addshards")
			debugConsole.register("addshards", "", { args, console ->
				val num = args[0].toInt()
				entityData.ascensionShards += num
				createHeroTable(entity, entityData, cameFromFaction)
				true
			})
		}

		mainTable.clear()

		mainTable.add(GameDataBar()).growX()
		mainTable.row()

		val stats = entity.stats()

		// faction, ascension, armour type
		val factionAscensionTable = Table()
		factionAscensionTable.add(SpriteWidget(stats.factionData!!.icon.copy(), 32f, 32f).addTapToolTip("Part of the '${stats.factionData!!.name}' faction."))

		val ascensionSprite = SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = stats.ascension.colour), 48f, 48f)
		factionAscensionTable.add(ascensionSprite.addTapToolTip("Ascension level " + (stats.ascension.ordinal + 1)))

		factionAscensionTable.add(SpriteWidget(stats.equipmentWeight.icon.copy(), 32f, 32f).addTapToolTip("Wears ${stats.equipmentWeight.niceName} equipment."))
		factionAscensionTable.row()

		mainTable.add(factionAscensionTable).expandX().center().pad(5f)
		mainTable.row()

		val ascensionLabel = Label(stats.ascension.toString().neaten(), Statics.skin)
		ascensionLabel.color = stats.ascension.colour.color()

		val rarityLabel = Label(entityData.factionEntity.rarity.niceName, Statics.skin)
		rarityLabel.color = entityData.factionEntity.rarity.colour.color()

		val ascensionRarityTable = Table()
		ascensionRarityTable.add(ascensionLabel).padRight(10f)
		ascensionRarityTable.add(rarityLabel)

		mainTable.add(ascensionRarityTable)
		mainTable.row()

		// name
		val nameLabel = Label(entity.name().name, Statics.skin, "title")
		mainTable.add(nameLabel).expandX().center().padTop(5f)
		mainTable.row()
		mainTable.add(Label(entity.name().title, Statics.skin, "small").tint(Color.LIGHT_GRAY)).expandX().center().padTop(2f).padBottom(5f)
		mainTable.row()

		// sprite and skills
		val spriteAndSkillsTable = Table()
		mainTable.add(spriteAndSkillsTable).grow().padTop(5f).padBottom(5f)
		mainTable.row()

		val leftSkillsColumn = Table()
		val rightSkillsColumn = Table()

		val abilities = entity.ability() ?: AbilityComponent.obtain()
		fun addAbilityIcon(index: Int, column: Table)
		{
			if (abilities.abilities.size > index)
			{
				val ability = abilities.abilities[index]
				val imageStack = Stack()

				val tileSprite = AssetManager.loadSprite("Icons/Active")
				imageStack.add(SpriteWidget(tileSprite, 32f, 32f))
				imageStack.add(SpriteWidget(ability.icon.copy(), 32f, 32f))
				imageStack.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 32f, 32f))

				imageStack.addTapToolTip(ability.name + "\n\n" + ability.description)

				column.add(imageStack).size(48f).pad(5f)
				column.row()
			}
			else
			{
				val emptySprite = AssetManager.loadSprite("Icons/Empty", colour = Colour(0.3f, 0.3f, 0.3f, 1.0f))
				val widget = SpriteWidget(emptySprite, 32f, 32f)

				column.add(widget).size(48f).pad(5f)
				column.row()
			}
		}
		addAbilityIcon(0, leftSkillsColumn)
		addAbilityIcon(1, rightSkillsColumn)
		addAbilityIcon(2, leftSkillsColumn)
		addAbilityIcon(3, rightSkillsColumn)
		addAbilityIcon(4, leftSkillsColumn)
		addAbilityIcon(5, rightSkillsColumn)

		spriteAndSkillsTable.add(leftSkillsColumn).growY()

		val spriteWidget = SpriteWidget(entity.renderable().renderable.copy() as Sprite, 32f, 32f)
		spriteAndSkillsTable.add(spriteWidget).expand().size(128f).center()
		spriteAndSkillsTable.add(rightSkillsColumn).growY()

		mainTable.add(Seperator(Statics.skin)).growX()
		mainTable.row()

		// equipment
		val equipmentTable = Table()
		mainTable.add(equipmentTable).growX().pad(5f)
		mainTable.row()

		var hasEquipment = false
		for (slot in EquipmentSlot.Values)
		{
			val equip = stats.equipment[slot]

			if (equip != null)
			{
				hasEquipment = true

				val tile = equip.createTile(48f)

				tile.addClickListener {

					val card = CardWidget(equip.createCardTable(entity), equip.createCardTable(entity), equip.icon.base, null, border = equip.ascension.colour)
					card.setFacing(true, false)

					card.addPick("Change", {
						createEquipmentTable(entity, entityData, slot, {
							recreateHeroesTable()
							createHeroTable(entity, entityData, cameFromFaction)
						})
					})

					card.addPick("Enhance", {

					})

					card.addPick("Remove", {
						val equip = stats.equipment[slot]
						if (equip != null)
						{
							Global.data.equipment.add(equip)
							stats.equipment[slot] = null
							entityData.equipment[slot] = null

							recreateHeroesTable()
							createHeroTable(entity, entityData, cameFromFaction)
						}
					})

					card.collapseFun = {
						card.remove()
					}

					stage.addActor(card)
					CardWidget.layoutCards(gdxArrayOf(card), Direction.CENTER)
					card.focus()
				}

				equipmentTable.add(tile).size(48f).expandX().center()
			}
			else
			{
				val equipmentStack = Stack()
				val tileBack = SpriteWidget(AssetManager.loadSprite("Icons/Empty"), 48f, 48f)
				equipmentStack.add(tileBack)

				val tileFront = SpriteWidget(slot.icon.copy(), 48f, 48f)
				tileFront.color = Color(0.6f, 0.6f, 0.6f, 0.5f)

				equipmentStack.add(tileFront)

				equipmentTable.add(equipmentStack).size(48f).expandX().center()

				equipmentStack.addClickListener {
					createEquipmentTable(entity, entityData, slot, {
						recreateHeroesTable()
						createHeroTable(entity, entityData, cameFromFaction)
					})
				}
			}
		}

		val removeEquipButton = TextButton("Remove Equipment", Statics.skin)
		if (hasEquipment)
		{
			removeEquipButton.addClickListener {
				for (slot in EquipmentSlot.Values)
				{
					val equip = stats.equipment[slot] ?: continue
					Global.data.equipment.add(equip)
					stats.equipment[slot] = null
					entityData.equipment[slot] = null
				}

				recreateHeroesTable()
				createHeroTable(entity, entityData, cameFromFaction)
			}
		}
		else
		{
			removeEquipButton.color = Color.DARK_GRAY
		}

		val optimiseEquipButton = TextButton("Optimise Equipment", Statics.skin)
		if (!stats.hasBestEquipment(entity))
		{
			optimiseEquipButton.addClickListener {
				for (slot in EquipmentSlot.Values)
				{
					val valid = Array<Equipment>()
					if (stats.equipment[slot] != null) valid.add(stats.equipment[slot])

					for (equip in Global.data.equipment)
					{
						if (equip.slot == slot && equip.weight == stats.equipmentWeight)
						{
							valid.add(equip)
						}
					}

					if (valid.size > 0)
					{
						if (stats.equipment[slot] != null) Global.data.equipment.add(stats.equipment[slot])

						val best = valid.maxBy { it.calculatePowerRating(entity) }
						stats.equipment[slot] = best
						entityData.equipment[slot] = best
						Global.data.equipment.removeValue(best, true)
					}
				}

				recreateHeroesTable()
				createHeroTable(entity, entityData, cameFromFaction)
			}
		}
		else
		{
			optimiseEquipButton.color = Color.DARK_GRAY
		}

		val equipButtonsTable = Table()
		equipButtonsTable.add(removeEquipButton).expandX().left()
		equipButtonsTable.add(optimiseEquipButton).expandX().right()
		mainTable.add(equipButtonsTable).growX().pad(5f)
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX()
		mainTable.row()

		// stats
		val statsAndPowerTable = Table()
		mainTable.add(statsAndPowerTable).growX()
		mainTable.row()

		statsAndPowerTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))

		val powerRatingTable = Table()
		powerRatingTable.addTapToolTip("Strength Rating (calculated from stats).")

		val powerRatingLabel = Label(stats.calculatePowerRating(entity).toInt().prettyPrint(), Statics.skin)
		powerRatingLabel.color = Color.GOLD

		powerRatingTable.add(SpriteWidget(Sprite(AssetManager.loadTextureRegion("Icons/upgrade")!!), 16f, 16f).tint(Color.GOLD)).padRight(3f)
		powerRatingTable.add(powerRatingLabel)

		statsAndPowerTable.add(powerRatingTable).expandX().center().padTop(5f)
		statsAndPowerTable.row()

		val statsTable = Table()
		statsAndPowerTable.add(statsTable).growX().pad(5f)
		statsAndPowerTable.row()

		fun addSubtextNumber(text: String, value: String): Table
		{
			val table = Table()
			table.add(Label(value, Statics.skin)).expandX().center()
			table.row()
			val textLabel = Label(text, Statics.skin, "small")
			textLabel.color = Color.GRAY
			table.add(textLabel).expandX().center()

			statsTable.add(table).expandX()

			return table
		}

		val currentLevelTable = addSubtextNumber("Level", stats.level.prettyPrint())
		val currentAscensionTable = addSubtextNumber("Ascension", (stats.ascension.ordinal+1).toString())
		val currentHealthTable = addSubtextNumber("Health", stats.getStat(Statistic.MAXHP).toInt().prettyPrint())
		val currentPowerTable =	addSubtextNumber("Power", stats.getStat(Statistic.POWER).toInt().prettyPrint())

		val fullStats = SpriteWidget(AssetManager.loadSprite("GUI/attack_empty"), 32f, 32f)
		fullStats.addClickListener {
			val table = Table()
			var bright = true
			for (stat in Statistic.Values)
			{
				val rawValue = stats.getStat(stat)

				val value: Int
				val valueStr: String
				if (Statistic.BaseValues.contains(stat))
				{
					value = rawValue.toInt()
					valueStr = value.prettyPrint()
				}
				else
				{
					value = (rawValue * 100f).toInt()
					valueStr = value.prettyPrint() + "%"
				}

				if (value != 0 || Statistic.CoreValues.contains(stat))
				{
					val rowTable = Table()
					rowTable.add(Label(stat.niceName, Statics.skin, "small")).pad(15f)
					rowTable.add(Label(valueStr, Statics.skin, "small")).expandX().right().pad(15f)
					rowTable.addTapToolTip(stat.niceName + ":\n\n" + stat.tooltip)

					if (bright)
					{
						rowTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
					}

					table.add(rowTable).growX()
					table.row()

					bright = !bright
				}
			}

			table.add(Table()).grow()

			FullscreenTable.createCloseable(table)
		}
		statsTable.add(fullStats).size(32f).expandX().right().padRight(5f)

		// level up / ascend button
		val levelButtonTable = Table()
		mainTable.add(levelButtonTable).growX().pad(5f)
		mainTable.row()

		class StatChangeData(val hp: Int, val power: Int, val rating: Int)
		var statChangeData: StatChangeData? = null
		fun preStatChanged()
		{
			statChangeData = StatChangeData(stats.getStat(Statistic.MAXHP).toInt(), stats.getStat(Statistic.POWER).toInt(), stats.calculatePowerRating(entity).toInt())
		}
		fun postStatChanged()
		{
			val newHp = stats.getStat(Statistic.MAXHP).toInt()
			val newPower = stats.getStat(Statistic.POWER).toInt()
			val rating = stats.calculatePowerRating(entity).toInt()

			val hpDiff = newHp - statChangeData!!.hp
			val powerDiff = newPower - statChangeData!!.power
			val ratingDiff = rating - statChangeData!!.rating

			val hpPopupLabel = Label("+" + hpDiff.prettyPrint(), Statics.skin).tint(Color.GREEN)
			hpPopupLabel.addAction(parallel(moveBy(0f, 20f, 2.5f), delay(2f), fadeOut(0.5f)) then removeActor())
			val hpPos = currentHealthTable.localToStageCoordinates(Vector2(0.5f, 1f))
			hpPopupLabel.setPosition(hpPos.x, hpPos.y + currentHealthTable.height*0.7f)
			stage.addActor(hpPopupLabel)

			val powerPopupLabel = Label("+" + powerDiff.prettyPrint(), Statics.skin).tint(Color.GREEN)
			powerPopupLabel.addAction(parallel(moveBy(0f, 20f, 2.5f), delay(2f), fadeOut(0.5f)) then removeActor())
			val powerPos = currentPowerTable.localToStageCoordinates(Vector2(0.5f, 1f))
			powerPopupLabel.setPosition(powerPos.x, powerPos.y + currentPowerTable.height*0.7f)
			stage.addActor(powerPopupLabel)

			val ratingPopupLabel = Label("+" + ratingDiff.prettyPrint(), Statics.skin).tint(Color.GOLD)
			ratingPopupLabel.addAction(parallel(moveBy(0f, 20f, 2.5f), delay(2f), fadeOut(0.5f)) then removeActor())
			val ratingPos = powerRatingTable.localToStageCoordinates(Vector2(0.5f, 1f))
			ratingPopupLabel.setPosition(ratingPos.x, ratingPos.y + powerRatingTable.height*0.7f)
			stage.addActor(ratingPopupLabel)
		}

		val levelTable = Table()
		val expRequired = Global.getExperienceForLevel(stats.level)
		if (expRequired <= Global.data.experience)
		{
			levelTable.add(Label("${Global.data.experience.prettyPrint()} / ${expRequired.prettyPrint()}", Statics.skin, "small")).expandX().center().pad(2f)
			levelTable.row()

			val levelButton = TextButton("Level Up", Statics.skin)
			levelButton.addClickListener {
				preStatChanged()

				entityData.level++
				stats.level = entityData.level

				postStatChanged()

				Global.data.experience -= expRequired

				val levelUpParticle = AssetManager.loadParticleEffect("LevelUp").getParticleEffect()
				levelUpParticle.size[0] = 5
				levelUpParticle.size[1] = 5
				val particleActor = ParticleEffectActor(levelUpParticle, true)
				particleActor.width = 256f / 5
				particleActor.height = 256f / 5

				val pos = spriteWidget.localToStageCoordinates(Vector2())
				particleActor.x = pos.x - 64f
				particleActor.y = pos.y - 96f

				stage.addActor(particleActor)

				recreateHeroesTable()
				createHeroTable(entity, entityData, cameFromFaction)
			}
			levelTable.add(levelButton).expandX().center()
		}
		else
		{
			levelTable.add(Label("[RED]${Global.data.experience.prettyPrint()}[] / ${expRequired.prettyPrint()}", Statics.skin, "small")).expandX().center().pad(2f)
			levelTable.row()
			val levelButton = TextButton("Level Up", Statics.skin)
			levelButton.color = Color.DARK_GRAY
			levelTable.add(levelButton).expandX().center()
		}

		val ascensionTable = Table()
		if (stats.ascension != Ascension.Values.last())
		{
			val nextAscension = stats.ascension.nextAscension

			if (entityData.ascensionShards >= nextAscension.shardsRequired)
			{
				ascensionTable.add(Label("${entityData.ascensionShards} / ${nextAscension.shardsRequired}", Statics.skin, "small")).expandX().center().pad(2f)
				ascensionTable.row()

				val ascendButton = NotificationButton("Ascend", Statics.skin)
				ascendButton.setNotification(AssetManager.loadTextureRegion("Icons/shard_notification")!!)

				ascendButton.addClickListener {
					if (stats.ascension != Ascension.Values.last())
					{
						val oldAscension = stats.ascension
						entityData.ascensionShards -= nextAscension.shardsRequired

						Future.call(
							{
								preStatChanged()

								entityData.ascension = nextAscension
								stats.ascension = entityData.ascension

								postStatChanged()

								recreateHeroesTable()
								createHeroTable(entity, entityData, cameFromFaction)
							}, 0.75f)

						val ascensionParticle = AssetManager.loadParticleEffect("Ascend").getParticleEffect()
						ascensionParticle.timeMultiplier = 2f
						val particleActor = ParticleEffectActor(ascensionParticle, true)
						particleActor.width = 64f
						particleActor.height = 64f

						particleActor.addAction(color(oldAscension.colour.color()) then color(nextAscension.colour.color(), 0.75f))

						val pos = ascensionSprite.localToStageCoordinates(Vector2())
						particleActor.x = pos.x - 8f
						particleActor.y = pos.y - 4f

						stage.addActor(particleActor)


						val ascendUpParticle = AssetManager.loadParticleEffect("AscendUp").getParticleEffect()
						ascendUpParticle.size[0] = 5
						ascendUpParticle.size[1] = 5
						val particleActor2 = ParticleEffectActor(ascendUpParticle, true)
						particleActor2.width = 256f / 5
						particleActor2.height = 256f / 5

						val pos2 = spriteWidget.localToStageCoordinates(Vector2())
						particleActor2.x = pos2.x - 64f
						particleActor2.y = pos2.y - 96f

						stage.addActor(particleActor2)

						recreateHeroesTable()
						createHeroTable(entity, entityData, cameFromFaction)
					}
				}
				ascensionTable.add(ascendButton).expandX().center()
			}
			else
			{
				ascensionTable.add(Label("[RED]${entityData.ascensionShards}[] / ${nextAscension.shardsRequired}", Statics.skin, "small")).expandX().center().pad(2f)
				ascensionTable.row()

				val ascendButton = TextButton("Ascend", Statics.skin)
				ascendButton.color = Color.DARK_GRAY
				ascensionTable.add(ascendButton).expandX().center()
			}
		}
		else
		{
			ascensionTable.add(Label("---", Statics.skin, "small")).expandX().center().pad(2f)
			ascensionTable.row()
			val ascensionButton = TextButton("Max Ascension", Statics.skin)
			ascensionButton.color = Color.DARK_GRAY
			ascensionTable.add(ascensionButton).expandX().center()
		}

		levelButtonTable.add(levelTable).expandX().center()
		levelButtonTable.add(ascensionTable).expandX().center()

		// back button
		val backButtonTable = Table()
		mainTable.add(backButtonTable).growX()
		mainTable.row()

		val backButton = TextButton("Back", Statics.skin)
		backButton.addClickListener {
			if (!cameFromFaction)
			{
				createHeroesTable()
			}
			else
			{
				createFactionTable(stats.factionData!!)
			}
		}
		backButtonTable.add(backButton).expandX().left().pad(5f)

		Save.save()
	}

	fun createEquipmentTable(entity: Entity, entityData: EntityData, slot: EquipmentSlot, refreshFunc: () -> Unit)
	{
		val stats = entity.stats()

		val table = Table()
		val fullscreenTable = FullscreenTable.createCloseable(table)

		val equipped = stats.equipment[slot]
		if (equipped != null)
		{
			table.add(Label("Equipped", Statics.skin, "title")).expandX().center().pad(10f)
			table.row()

			val equipTable = Table()

			equipTable.add(equipped.createTile(24f)).size(24f).pad(2f)
			equipTable.add(Label(equipped.fullName, Statics.skin, "small").wrap()).growX().pad(2f)
			equipTable.add(SpriteWidget(Sprite(AssetManager.loadTextureRegion("Icons/upgrade")!!), 16f, 16f).tint(Color.GOLD)).right()
			equipTable.add(Label(equipped.calculatePowerRating(entity).toInt().prettyPrint(), Statics.skin).tint(Color.GOLD)).pad(2f)

			equipTable.addClickListener {
				val card = CardWidget(equipped.createCardTable(entity), equipped.createCardTable(entity), equipped.icon.base, equipped, border = equipped.ascension.colour)
				card.collapseFun = {
					card.remove()
				}

				card.setFacing(true, false)
				stage.addActor(card)
				CardWidget.layoutCards(gdxArrayOf(card), Direction.CENTER)
				card.focus()
			}

			table.add(equipTable).growX()
			table.row()
		}

		val equipmentList = Table()

		val valid = Array<Equipment>()
		for (equip in Global.data.equipment)
		{
			if (equip.slot == slot && equip.weight == stats.equipmentWeight)
			{
				valid.add(equip)
			}
		}

		var bright = true
		val sorted = valid.sortedByDescending { it.calculatePowerRating(entity) }
		for (equip in sorted)
		{
			val equipTable = Table()

			equipTable.add(equip.createTile(24f)).size(24f).pad(5f)
			equipTable.add(Label(equip.fullName, Statics.skin, "small").wrap()).growX().pad(5f)
			equipTable.add(Label(equip.calculatePowerRating(entity).toInt().prettyPrint(), Statics.skin).tint(Color.GOLD)).pad(5f)

			if (bright)
			{
				equipTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
			}

			equipTable.addClickListener {
				val card = CardWidget(equip.createCardTable(entity), equip.createCardTable(entity), equip.icon.base, equip, border = equip.ascension.colour)
				card.addPick("Equip",
							 {
								 val existing = stats.equipment[slot]
								 if (existing != null)
								 {
									 Global.data.equipment.add(existing)
								 }

								 stats.equipment[slot] = equip
								 entityData.equipment[slot] = equip
								 Global.data.equipment.removeValue(equip, true)
								 fullscreenTable.remove()
								 card.remove()

								 refreshFunc.invoke()
							 })
				card.collapseFun = {
					card.remove()
				}

				card.setFacing(true, false)
				stage.addActor(card)
				CardWidget.layoutCards(gdxArrayOf(card), Direction.CENTER)
				card.focus()
			}

			equipmentList.add(equipTable).growX()
			equipmentList.row()

			bright = !bright
		}
		equipmentList.add(Table()).grow()

		val scroll = ScrollPane(equipmentList)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setOverscroll(false, false)
		scroll.setForceScroll(false, true)

		table.add(Label("Equipment", Statics.skin, "title")).expandX().center().pad(10f)
		table.row()
		table.add(scroll).grow()
	}

	val factionsContentTable = Table()
	fun recreateFactionsTable()
	{
		val contentTable = factionsContentTable
		factionsContentTable.clear()

		contentTable.add(GameDataBar()).growX()
		contentTable.row()

		val heroesButton = Table()
		heroesButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel"))
		heroesButton.add(Label("Heroes", Statics.skin)).center()
		heroesButton.addClickListener {
			createHeroesTable()
		}

		val factionsButton = Table()
		factionsButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanelHighlight"))
		factionsButton.add(Label("Factions", Statics.skin)).center()

		val navigationTable = Table()
		navigationTable.add(heroesButton).growY().width(Value.percentWidth(0.5f, navigationTable))
		navigationTable.add(factionsButton).growY().width(Value.percentWidth(0.5f, navigationTable))

		contentTable.add(navigationTable).growX().height(35f)
		contentTable.row()

		val factionsTable = Table()

		for (faction in Global.data.unlockedFactions)
		{
			val factionTable = Table()
			factionTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/Button.png"), 6, 6, 6, 6))

			factionTable.add(SpriteWidget(faction.icon, 16f, 16f)).pad(10f)
			factionTable.add(Label(faction.name, Statics.skin)).pad(10f).padTop(15f).padBottom(15f)

			val numUnlockedHeroes = faction.heroes.count { Global.data.heroPool.any { ed -> ed.factionEntity == it } }
			factionTable.add(Label(numUnlockedHeroes.toString() + "/" + faction.heroes.size, Statics.skin)).pad(5f).expandX().right()

			factionTable.addClickListener {
				createFactionTable(faction)
			}

			factionsTable.add(factionTable).pad(5f).growX()
			factionsTable.row()
		}

		factionsTable.add(Table()).grow()

		val scroll = ScrollPane(factionsTable)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setOverscroll(false, false)
		scroll.setForceScroll(false, true)

		contentTable.add(scroll).grow()
		contentTable.row()
		contentTable.add(NavigationBar(MainGame.ScreenEnum.HEROES)).growX()
	}

	fun createFactionsTable()
	{
		mainTable.clear()
		mainTable.add(factionsContentTable).grow()
	}

	fun createFactionTable(faction: Faction)
	{
		mainTable.clear()

		mainTable.add(GameDataBar()).growX()
		mainTable.row()

		// icon
		mainTable.add(SpriteWidget(faction.icon.copy(), 32f, 32f)).expandX().center().pad(20f)
		mainTable.row()

		// name
		mainTable.add(Label(faction.name, Statics.skin, "title")).expandX().center().pad(20f)
		mainTable.row()

		// description
		mainTable.add(Label(faction.description, Statics.skin).wrap()).growX().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(10f)
		mainTable.row()

		// buffs
		mainTable.add(Label("Buffs gained when 2 or more of this faction are chosen to battle.", Statics.skin, "small")).padTop(20f)
		mainTable.row()

		val buffsTable = Table()
		for (i in 0 until faction.buffs.size)
		{
			val stack = Stack()
			val widget = SpriteWidget(faction.icon.copy(), 32f, 32f)
			stack.addTapToolTip(faction.name + " ${i+2}:\n\n" + faction.buffs[i].description)

			val table = Table()
			table.add(Label((i+2).toString(), Statics.skin)).expand().bottom().right()

			stack.add(widget)
			stack.add(table)

			buffsTable.add(stack).size(32f).pad(15f)
		}
		mainTable.add(buffsTable).growX().padBottom(20f)
		mainTable.row()

		mainTable.add(Seperator(Statics.skin)).growX().pad(10f)
		mainTable.row()

		// heroes
		val heroesTable = Table()
		mainTable.add(heroesTable).grow().padTop(20f)
		mainTable.row()

		val heroesARow = 5
		var x = 0
		for (factionHero in faction.heroes)
		{
			val entityData = Global.data.heroPool.firstOrNull { it.factionEntity == factionHero }
			if (entityData != null)
			{
				val entity = entityData.getEntity("1")

				val heroWidget = HeroSelectionWidget(entity, entityData, true)
				heroWidget.addClickListener {
					createHeroTable(entity, entityData, true)
				}

				heroesTable.add(heroWidget).size((Statics.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
				x++
				if (x == heroesARow)
				{
					x = 0
					heroesTable.row()
				}
			}
			else
			{
				val heroWidget = SpriteWidget(AssetManager.loadSprite("Icons/Unknown"), 32f, 32f)
				heroWidget.addTapToolTip("Unknown hero. Rarity [GOLD]${factionHero.rarity.niceName}[].")

				heroesTable.add(heroWidget).size((Statics.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
				x++
				if (x == heroesARow)
				{
					x = 0
					heroesTable.row()
				}
			}
		}

		heroesTable.row()
		heroesTable.add(Table()).colspan(heroesARow).grow()

		// back button
		val backButtonTable = Table()
		mainTable.add(backButtonTable).growX()
		mainTable.row()

		val backButton = TextButton("Back", Statics.skin)
		backButton.addClickListener {
			createFactionsTable()
		}
		backButtonTable.add(backButton).expandX().left().pad(5f)
	}

	override fun doRender(delta: Float)
	{

	}
}