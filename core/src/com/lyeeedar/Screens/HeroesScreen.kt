package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
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
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.neaten
import com.lyeeedar.Util.prettyPrint
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

		debugConsole.register("addhero", "", fun (args, console): Boolean {

			if (args.size != 1)
			{
				console.error("must provide a hero name!")
				return false
			}

			for (faction in Global.data.unlockedFactions)
			{
				for (hero in faction.heroes)
				{
					val entity = EntityLoader.load(hero.entityPath)
					if (entity.name().name.toLowerCase() == args[0].toLowerCase())
					{
						Global.data.heroPool.add(EntityData(hero, Ascension.MUNDANE, 1))

						return true
					}
				}
			}

			return false
		})
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
		heroesButton.add(Label("Heroes", Global.skin)).center()

		val factionsButton = Table()
		factionsButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel"))
		factionsButton.add(Label("Factions", Global.skin)).center()
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
			val heroWidget = HeroSelectionWidget(hero.entity, hero.data.factionEntity)
			heroWidget.addClickListener {
				createHeroTable(hero.entity, hero.data, false)
			}

			heroesTable.add(heroWidget).size((Global.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
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
		mainTable.clear()

		mainTable.add(GameDataBar()).growX()
		mainTable.row()

		val stats = entity.stats()!!

		// faction, ascension, armour type
		val factionAscensionTable = Table()
		factionAscensionTable.add(SpriteWidget(stats.factionData!!.icon.copy(), 32f, 32f).addTapToolTip("Part of the '${stats.factionData!!.name}' faction."))

		factionAscensionTable.add(
			SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = stats.ascension.colour), 48f, 48f)
				.addTapToolTip("Ascension level " + (stats.ascension.ordinal + 1)))

		factionAscensionTable.add(SpriteWidget(stats.equipmentWeight.icon.copy(), 32f, 32f).addTapToolTip("Wears ${stats.equipmentWeight.niceName} equipment."))
		factionAscensionTable.row()

		val ascensionLabel = Label(stats.ascension.toString().neaten(), Global.skin)
		ascensionLabel.color = stats.ascension.colour.color()

		val rarityLabel = Label(entityData.factionEntity.rarity.niceName, Global.skin)
		rarityLabel.color = entityData.factionEntity.rarity.colour.color()
		factionAscensionTable.add(rarityLabel).colspan(3).center()

		mainTable.add(factionAscensionTable).expandX().center().pad(5f)
		mainTable.row()

		// name
		val nameLabel = Label(entity.name().name, Global.skin, "title")
		mainTable.add(nameLabel).expandX().center().padTop(5f)
		mainTable.row()
		mainTable.add(Label(entity.name().title, Global.skin, "small").tint(Color.LIGHT_GRAY)).expandX().center().padTop(2f).padBottom(5f)
		mainTable.row()

		// sprite and skills
		val spriteAndSkillsTable = Table()
		mainTable.add(spriteAndSkillsTable).growX().padTop(10f).padBottom(10f)
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

				column.add(imageStack).size(48f).pad(10f)
				column.row()
			}
			else
			{
				val emptySprite = AssetManager.loadSprite("Icons/Empty", colour = Colour(0.3f, 0.3f, 0.3f, 1.0f))
				val widget = SpriteWidget(emptySprite, 32f, 32f)

				column.add(widget).size(48f).pad(10f)
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
		spriteAndSkillsTable.add(SpriteWidget(entity.renderable().renderable.copy() as Sprite, 32f, 32f)).expand().size(128f).center()
		spriteAndSkillsTable.add(rightSkillsColumn).growY()

		mainTable.add(Seperator(Global.skin)).growX()
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

		val removeEquipButton = TextButton("Remove Equipment", Global.skin)
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

		val optimiseEquipButton = TextButton("Optimise Equipment", Global.skin)
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

		val equipButtonsTable = Table()
		equipButtonsTable.add(removeEquipButton).expandX().left()
		equipButtonsTable.add(optimiseEquipButton).expandX().right()
		mainTable.add(equipButtonsTable).growX().pad(5f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		// stats
		val statsAndPowerTable = Table()
		mainTable.add(statsAndPowerTable).growX()
		mainTable.row()

		statsAndPowerTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))

		val powerRatingLabel = Label(stats.calculatePowerRating(entity).toInt().prettyPrint(), Global.skin)
		powerRatingLabel.color = Color.GOLD
		powerRatingLabel.addTapToolTip("Strength Rating (calculated from stats).")

		statsAndPowerTable.add(powerRatingLabel).expandX().center().padTop(5f)
		statsAndPowerTable.row()

		val statsTable = Table()
		statsAndPowerTable.add(statsTable).growX().pad(5f)
		statsAndPowerTable.row()

		fun addSubtextNumber(text: String, value: String)
		{
			val table = Table()
			table.add(Label(value, Global.skin)).expandX().center()
			table.row()
			val textLabel = Label(text, Global.skin, "small")
			textLabel.color = Color.GRAY
			table.add(textLabel).expandX().center()

			statsTable.add(table).expandX()
		}

		addSubtextNumber("Level", stats.level.prettyPrint())
		addSubtextNumber("Ascension", stats.ascension.name.neaten())
		addSubtextNumber("Health", stats.getStat(Statistic.MAXHP).toInt().prettyPrint())
		addSubtextNumber("Power", stats.getStat(Statistic.POWER).toInt().prettyPrint())

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
					rowTable.add(Label(stat.niceName, Global.skin, "small")).pad(15f)
					rowTable.add(Label(valueStr, Global.skin, "small")).expandX().right().pad(15f)
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

		val levelTable = Table()
		val expRequired = Global.getExperienceForLevel(stats.level)
		if (expRequired < Global.data.experience)
		{
			levelTable.add(Label("${expRequired.prettyPrint()} / ${Global.data.experience.prettyPrint()}", Global.skin, "small")).expandX().center().pad(2f)
			levelTable.row()

			val levelButton = TextButton("Level Up", Global.skin)
			levelButton.addClickListener {
				entityData.level++
				stats.level = entityData.level

				Global.data.experience -= expRequired

				recreateHeroesTable()
				createHeroTable(entity, entityData, cameFromFaction)
			}
			levelTable.add(levelButton).expandX().center()
		}
		else
		{
			levelTable.add(Label("[RED]${expRequired.prettyPrint()}[] / ${Global.data.experience.prettyPrint()}", Global.skin, "small")).expandX().center().pad(2f)
			levelTable.row()
			val levelButton = TextButton("Level Up", Global.skin)
			levelButton.color = Color.DARK_GRAY
			levelTable.add(levelButton).expandX().center()
		}

		val ascensionTable = Table()
		if (stats.ascension != Ascension.Values.last())
		{
			val nextAscension = Ascension.Values[Ascension.Values.indexOf(entityData.ascension) + 1]

			if (entityData.ascensionShards >= nextAscension.shardsRequired)
			{
				ascensionTable.add(Label("${nextAscension.shardsRequired} / ${entityData.ascensionShards}", Global.skin, "small")).expandX().center().pad(2f)
				ascensionTable.row()

				val ascendButton = TextButton("Ascend", Global.skin)

				ascendButton.addClickListener {
					if (stats.ascension != Ascension.Values.last())
					{
						entityData.ascensionShards -= nextAscension.shardsRequired

						entityData.ascension = nextAscension
						stats.ascension = entityData.ascension

						recreateHeroesTable()
						createHeroTable(entity, entityData, cameFromFaction)
					}
				}
				ascensionTable.add(ascendButton).expandX().center()
			}
			else
			{
				ascensionTable.add(Label("[RED]${entityData.ascensionShards}[] / ${nextAscension.shardsRequired}", Global.skin, "small")).expandX().center().pad(2f)
				ascensionTable.row()

				val ascendButton = TextButton("Ascend", Global.skin)
				ascendButton.color = Color.DARK_GRAY
				ascensionTable.add(ascendButton).expandX().center()
			}
		}
		else
		{
			ascensionTable.add(Label("---", Global.skin, "small")).expandX().center().pad(2f)
			ascensionTable.row()
			val ascensionButton = TextButton("Max Ascension", Global.skin)
			ascensionButton.color = Color.DARK_GRAY
			ascensionTable.add(ascensionButton).expandX().center()
		}

		levelButtonTable.add(levelTable).expandX().center()
		levelButtonTable.add(ascensionTable).expandX().center()

		// back button
		val backButtonTable = Table()
		mainTable.add(backButtonTable).growX()
		mainTable.row()

		val backButton = TextButton("Back", Global.skin)
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
	}

	fun createEquipmentTable(entity: Entity, entityData: EntityData, slot: EquipmentSlot, refreshFunc: () -> Unit)
	{
		val stats = entity.stats()

		val table = Table()
		val fullscreenTable = FullscreenTable.createCloseable(table)

		val equipped = stats.equipment[slot]
		if (equipped != null)
		{
			table.add(Label("Equipped", Global.skin, "title")).expandX().center().pad(10f)
			table.row()

			val equipTable = Table()

			equipTable.add(equipped.createTile(24f)).size(24f).pad(2f)
			equipTable.add(Label(equipped.fullName, Global.skin, "small")).pad(2f)
			equipTable.add(Label(equipped.calculatePowerRating(entity).toInt().prettyPrint(), Global.skin).tint(Color.GOLD)).expandX().right().pad(2f)

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
			equipTable.add(Label(equip.fullName, Global.skin, "small")).pad(5f)
			equipTable.add(Label(equip.calculatePowerRating(entity).toInt().prettyPrint(), Global.skin).tint(Color.GOLD)).expandX().right().pad(5f)

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

		table.add(Label("Equipment", Global.skin, "title")).expandX().center().pad(10f)
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
		heroesButton.add(Label("Heroes", Global.skin)).center()
		heroesButton.addClickListener {
			createHeroesTable()
		}

		val factionsButton = Table()
		factionsButton.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanelHighlight"))
		factionsButton.add(Label("Factions", Global.skin)).center()

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
			factionTable.add(Label(faction.name, Global.skin)).pad(10f).padTop(15f).padBottom(15f)

			val numUnlockedHeroes = faction.heroes.count { Global.data.heroPool.any { ed -> ed.factionEntity == it } }
			factionTable.add(Label(numUnlockedHeroes.toString() + "/" + faction.heroes.size, Global.skin)).pad(5f).expandX().right()

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
		mainTable.add(Label(faction.name, Global.skin, "title")).expandX().center().pad(20f)
		mainTable.row()

		// description
		mainTable.add(Label(faction.description, Global.skin).wrap()).growX().pad(20f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX().pad(10f)
		mainTable.row()

		// buffs
		mainTable.add(Label("Buffs gained when 2 or more of this faction are chosen to battle.", Global.skin, "small")).padTop(20f)
		mainTable.row()

		val buffsTable = Table()
		for (i in 0 until faction.buffs.size)
		{
			val stack = Stack()
			val widget = SpriteWidget(faction.icon.copy(), 32f, 32f)
			stack.addTapToolTip(faction.name + " ${i+2}:\n\n" + faction.buffs[i].description)

			val table = Table()
			table.add(Label((i+2).toString(), Global.skin)).expand().bottom().right()

			stack.add(widget)
			stack.add(table)

			buffsTable.add(stack).size(32f).pad(15f)
		}
		mainTable.add(buffsTable).growX().padBottom(20f)
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX().pad(10f)
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

				val heroWidget = HeroSelectionWidget(entity, entityData.factionEntity)
				heroWidget.addClickListener {
					createHeroTable(entity, entityData, true)
				}

				heroesTable.add(heroWidget).size((Global.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
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

				heroesTable.add(heroWidget).size((Global.resolution.x - (heroesARow * 2f) - 10f) / heroesARow.toFloat()).pad(2f)
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

		val backButton = TextButton("Back", Global.skin)
		backButton.addClickListener {
			createFactionsTable()
		}
		backButtonTable.add(backButton).expandX().left().pad(5f)
	}

	override fun doRender(delta: Float)
	{

	}
}