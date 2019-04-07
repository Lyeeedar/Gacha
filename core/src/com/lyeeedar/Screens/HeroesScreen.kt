package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Ascension
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Faction
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class HeroesScreen : AbstractScreen()
{
	override fun create()
	{
		drawFPS = false

		mainTable.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion("Oryx/uf_split/uf_terrain/floor_extra_15"))).tint(Color(0.2f, 0.2f, 0.2f, 1f))

		recreateFactionsTable()
		recreateHeroesTable()
		createHeroesTable()
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
		val allHeroes = com.badlogic.gdx.utils.Array<Entity>()
		for (heroData in Global.data.heroPool)
		{
			val hero = heroData.getEntity("1")
			allHeroes.add(hero)
		}

		val heroesTable = Table()

		val heroesARow = 5
		var x = 0
		for (hero in allHeroes.sortedByDescending { (it.stats().getStat(Statistic.POWER) * 10) + it.stats().getStat(Statistic.MAXHP) })
		{
			val heroWidget = HeroSelectionWidget(hero)
			heroWidget.addClickListener {
				createHeroTable(hero, false)
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
		contentTable.add(NavigationBar(MainGame.ScreenEnum.HEROES)).growX().height(75f)
	}

	fun createHeroesTable()
	{
		mainTable.clear()
		mainTable.add(heroesContentTable).grow()
	}

	fun createHeroTable(entity: Entity, cameFromFaction: Boolean)
	{
		mainTable.clear()

		val stats = entity.stats()!!

		// faction, ascension, armour type
		val factionAscensionTable = Table()
		factionAscensionTable.add(SpriteWidget(stats.factionData!!.icon.copy(), 32f, 32f).addTapToolTip("Part of the '${stats.factionData!!.name}' faction."))

		factionAscensionTable.add(
			SpriteWidget(AssetManager.loadSprite("GUI/ascensionBar", colour = stats.ascension.colour), 48f, 48f)
				.addTapToolTip("Ascension level " + (Ascension.values().indexOfFirst { it == stats.ascension } + 1)))

		factionAscensionTable.add(SpriteWidget(AssetManager.loadSprite("GUI/attack_empty"), 32f, 32f))
		factionAscensionTable.row()

		val ascensionLabel = Label(stats.ascension.toString().toLowerCase().capitalize(), Global.skin)
		ascensionLabel.color = stats.ascension.colour.color()
		factionAscensionTable.add(ascensionLabel).colspan(3).center()

		mainTable.add(factionAscensionTable).expandX().center().pad(5f)
		mainTable.row()

		// name
		val nameLabel = Label(entity.name().name, Global.skin, "title")
		mainTable.add(nameLabel).expandX().center().pad(15f)
		mainTable.row()

		// sprite and skills
		val spriteAndSkillsTable = Table()
		mainTable.add(spriteAndSkillsTable).growX().padTop(40f).padBottom(20f)
		mainTable.row()

		val leftSkillsColumn = Table()
		val rightSkillsColumn = Table()

		val abilities = entity.ability() ?: AbilityComponent()
		fun addAbilityIcon(index: Int, column: Table)
		{
			if (abilities.abilities.size > index)
			{
				val ability = abilities.abilities[index]
				val imageStack = Stack()

				val tileSprite = AssetManager.loadSprite("Icons/Active")
				imageStack.add(SpriteWidget(tileSprite, 32f, 32f))
				imageStack.add(SpriteWidget(ability.icon.copy(), 32f, 32f))

				//imageStack.addTapToolTip(ability.description)

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
		mainTable.add(equipmentTable).grow()
		mainTable.row()

		mainTable.add(Seperator(Global.skin)).growX()
		mainTable.row()

		// stats
		val statsTable = Table()
		mainTable.add(statsTable).growX()
		mainTable.row()

		// level up / ascend button
		val levelButtonTable = Table()
		mainTable.add(levelButtonTable).growX()
		mainTable.row()

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

	val factionsContentTable = Table()
	fun recreateFactionsTable()
	{
		val contentTable = factionsContentTable
		factionsContentTable.clear()

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
			val panelBack = AssetManager.loadTextureRegion("GUI/TilePanel")
			factionTable.background = NinePatchDrawable(NinePatch(panelBack, 12, 12, 12, 12))

			factionTable.add(SpriteWidget(faction.icon, 16f, 16f)).pad(3f)
			factionTable.add(Label(faction.name, Global.skin)).padLeft(10f)
			factionTable.add(Label("2/6", Global.skin)).expandX().right()

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
		contentTable.add(NavigationBar(MainGame.ScreenEnum.HEROES)).growX().height(75f)
	}

	fun createFactionsTable()
	{
		mainTable.clear()
		mainTable.add(factionsContentTable).grow()
	}

	fun createFactionTable(faction: Faction)
	{

	}

	override fun doRender(delta: Float)
	{

	}
}