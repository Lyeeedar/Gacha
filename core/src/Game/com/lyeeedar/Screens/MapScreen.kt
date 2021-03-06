package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.*
import com.lyeeedar.Game.*
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.AbstractSystem
import com.lyeeedar.Systems.systemList
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.prettyPrint
import ktx.actors.then
import ktx.collections.set
import ktx.collections.toGdxArray

class MapScreen : AbstractScreen()
{
	lateinit var batch: SpriteBatch

	var level: Level? = null
	var zone: Zone? = null

	class TimeMultiplier(val name: String, val multiplier: Float)
	val multipliers = arrayOf(TimeMultiplier("x0.5", 0.5f), TimeMultiplier("x1", 0.8f), TimeMultiplier("x2", 1.3f))
	var multiplierIndex = 1
	var paused = false

	data class SystemData(val name: String, var time: Float, var entities: String)
	val systemTimes: Array<SystemData> by lazy { Array<SystemData>(systemList.size) { i -> SystemData(systemList[i].java.simpleName.replace("System", ""), 0f, "--") } }
	var totalSystemTime: Float = 0f
	var drawSystemTime = false

	var clickedTile: Tile? = null
	var selectedEntity: Entity? = null
	var selectedComponent: IDebugCommandProvider? = null

	var systemUpdateAccumulator = 0f

	val bottomArea = Table()
	val aboveGridTable = Table()
	val heroesTable = Table()
	val heroWidgets = com.badlogic.gdx.utils.Array<HeroSelectionWidget>()
	val factionBonusTable = Table()

	var levelComplete = false

	override fun create()
	{
		batch = SpriteBatch()

		debugConsole.register("SystemDebug", "'SystemDebug true' to enable, 'SystemDebug false' to disable", fun (args, console): Boolean {
			if (args[0] == "false")
			{
				drawSystemTime = false
				return true
			}
			else if (args[0] == "true")
			{
				drawSystemTime = true
				return true
			}

			return false
		})

		debugConsole.register("Select", "", fun (args, console): Boolean {
			if (clickedTile == null)
			{
				console.error("No tile selected!")
			}
			else if (args.size == 1)
			{
				if (selectedEntity != null)
				{
					val componentName = args[0] + "component"
					for (component in selectedEntity!!.components)
					{
						if (component.javaClass.simpleName.toLowerCase() == componentName)
						{
							if (component is IDebugCommandProvider)
							{
								selectedComponent = component
								component.attachCommands(debugConsole)

								console.write("")
								console.write("Selected component: " + args[0])

								return true
							}
							else
							{

								console.error("Component has no debugging commands!")
								return false
							}
						}
					}
				}

				val slot = SpaceSlot.valueOf(args[0].toUpperCase())

				if (clickedTile!!.contents.containsKey(slot))
				{
					selectedEntity = clickedTile!!.contents[slot]
					selectedComponent?.detachCommands(debugConsole)
					selectedComponent = null

					console.write("")
					console.write("Selected entity: " + selectedEntity!!.name().name)
					for (component in selectedEntity!!.components)
					{
						val isProvider = component is IDebugCommandProvider
						console.write(component.javaClass.simpleName.replace("Component", "") + if (isProvider) " - Debug" else "")
					}

					return true
				}
				else
				{
					console.error("No entity in that slot on the selected tile!")
				}
			}
			else
			{
				console.error("Too many arguments!")
			}

			return false
		})
	}

	fun setNewLevel(zone: Zone)
	{
		if ( !created )
		{
			baseCreate()
			created = true
		}

		levelComplete = false
		this.zone = zone
		val currentEncounter = zone.encounters[Global.data.currentZoneProgression]

		val level = currentEncounter.createLevel(zone.theme)

		this.level = level
		Global.changeLevel(level)

		val mapWidget = RenderSystemWidget()

		val gridStack = Stack()
		gridStack.add(mapWidget)
		gridStack.add(aboveGridTable)

		mainTable.clear()
		mainTable.background = TiledDrawable(TextureRegionDrawable(level.theme.backgroundTile)).tint(level.ambient.color())
		mainTable.add(gridStack).grow().height(Value.percentHeight(0.7f, mainTable))
		mainTable.row()
		mainTable.add(bottomArea).grow().height(Value.percentHeight(0.3f, mainTable))

		selectEntities()
	}

	fun beginLevel()
	{
		for (i in 0 until 5)
		{
			Global.data.lastSelectedHeroes[i] = level!!.playerTiles[i].entity?.name()?.name
		}

		bottomArea.clear()
		aboveGridTable.clear()

		val entityTable = Table()
		entityTable.background(TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0.1f, 0.1f, 0.1f, 1.0f)))
		entityTable.defaults().uniform().pad(1f)

		for (ent in level!!.playerTiles)
		{
			val widget = EntityWidget(ent.entity)
			entityTable.add(widget).grow()
		}

		val pauseButton = Button(Statics.skin, "play")
		pauseButton.addClickListener {
			paused = !paused

			if (paused)
			{
				pauseButton.style = Statics.skin.get("pause", Button.ButtonStyle::class.java)
			}
			else
			{
				pauseButton.style = Statics.skin.get("play", Button.ButtonStyle::class.java)
			}
		}

		val speedButton = TextButton(multipliers[multiplierIndex].name, Statics.skin)
		speedButton.addClickListener {
			multiplierIndex++
			if (multiplierIndex == multipliers.size)
			{
				multiplierIndex = 0
			}

			speedButton.setText(multipliers[multiplierIndex].name)
		}

		val buttonTable = Table()
		buttonTable.add(pauseButton).pad(5f).size(30f)
		buttonTable.add(speedButton).pad(5f).height(30f)
		buttonTable.add(Table()).growX()

		bottomArea.add(buttonTable).growX().expandY().bottom()
		bottomArea.row()
		bottomArea.add(Seperator(Statics.skin, false)).growX()
		bottomArea.row()
		bottomArea.add(entityTable).growX().height(75f)

		level!!.begin()
	}

	fun selectEntities()
	{
		level!!.selectingEntities = true

		bottomArea.clear()
		aboveGridTable.clear()

		val bonusTable = Table()
		aboveGridTable.add(bonusTable).colspan(2).growX()
		aboveGridTable.row()
		aboveGridTable.add(Table()).colspan(2).grow()

		bonusTable.add(factionBonusTable)
		bonusTable.add(Table()).growX()
		val enemyBonusTable = Table()
		bonusTable.add(enemyBonusTable)
		bonusTable.row()

		class FactionCount(val faction: Faction, var count: Int)
		val enemyFactions = ObjectMap<String, FactionCount>()
		for (tile in level!!.enemyTiles)
		{
			val entity = tile.entity ?: continue

			val stats = entity.statsOrNull() ?: continue
			val factionData = stats.factionData ?: continue

			if (stats.faction == "2")
			{
				var faction = enemyFactions[factionData.name]
				if (faction == null)
				{
					faction = FactionCount(factionData, 0)
					enemyFactions[factionData.name] = faction
				}

				faction.count++
			}
		}

		for (faction in enemyFactions)
		{
			for (i in 0 until faction.value.faction.buffs.size)
			{
				if (faction.value.count >= 2+i)
				{
					val stack = Stack()
					val widget = SpriteWidget(faction.value.faction.icon.copy(), 24f, 24f)
					stack.addTapToolTip(faction.value.faction.name + " ${i+2}:\n\n" + faction.value.faction.buffs[i].description)

					val table = Table()
					table.add(Label((i+2).toString(), Statics.skin, "small")).expand().bottom().right()

					stack.add(widget)
					stack.add(table)

					enemyBonusTable.add(stack).size(24f).pad(5f)
				}
			}
		}

		bottomArea.add(Seperator(Statics.skin, false)).growX()
		bottomArea.row()

		heroesTable.defaults().uniform().pad(1f)

		val scrollTable = Table()
		scrollTable.background(TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0.1f, 0.1f, 0.1f, 1.0f)))

		val scroll = ScrollPane(heroesTable)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setOverscroll(false, false)
		scroll.setForceScroll(false, true)
		scrollTable.add(scroll).grow()
		scrollTable.row()
		scrollTable.add(Seperator(Statics.skin)).growX()

		bottomArea.add(scrollTable).grow()
		bottomArea.row()

		heroWidgets.clear()
		heroesTable.clear()

		class EntityAndData(val data: EntityData, val entity: Entity)
		val allHeroes = com.badlogic.gdx.utils.Array<EntityAndData>()
		for (heroData in Global.data.heroPool)
		{
			val hero = heroData.getEntity("1")
			allHeroes.add(EntityAndData(heroData, hero))
		}

		val heroesARow = 5
		var x = 0
		for (hero in allHeroes.sortedByDescending { it.entity.stats().calculatePowerRating(it.entity) })
		{
			val heroWidget = HeroSelectionWidget(hero.entity, hero.data)
			for (existing in level!!.playerTiles)
			{
				val ent = existing.entity ?: continue
				if (ent.loadData()!!.path == hero.entity.loadData()!!.path)
				{
					heroWidget.alreadyUsed = true
					break
				}
			}
			heroWidget.addClickListener {
				if (!heroWidget.alreadyUsed)
				{
					for (existing in level!!.playerTiles)
					{
						if (existing.entity == null)
						{
							existing.entity = hero.entity
							hero.entity.pos().tile = existing.tile
							hero.entity.pos().addToTile(hero.entity)
							Global.engine.addEntity(hero.entity)
							break
						}
					}
					updateHeroWidgets()
				}
			}

			heroWidgets.add(heroWidget)
			heroesTable.add(heroWidget).size((Statics.resolution.x - 10) / heroesARow.toFloat())
			x++
			if (x == heroesARow)
			{
				x = 0
				heroesTable.row()
			}
		}

		for (i in 0 until 5)
		{
			val lastSelectedHero = Global.data.lastSelectedHeroes[i]
			if (lastSelectedHero != null)
			{
				val hero = allHeroes.firstOrNull { it.entity.name().name == lastSelectedHero }
				if (hero != null)
				{
					val slot = level!!.playerTiles[i]
					slot.entity = hero.entity

					hero.entity.pos().tile = slot.tile
					hero.entity.pos().addToTile(hero.entity)
					Global.engine.addEntity(hero.entity)
				}
			}
		}

		updateHeroWidgets()

		val backTable = Table()
		val backButton = TextButton("Back", Statics.skin)
		backButton.addClickListener {
			Statics.game.switchScreen(MainGame.ScreenEnum.ZONE)
		}
		backTable.add(backButton).expandX().left().pad(2f)

		val beginTable = Table()
		beginTable.background(TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0.1f, 0.1f, 0.1f, 1.0f)))

		val beginButton = TextButton("Begin", Statics.skin)
		beginButton.addClickListener {
			if (level!!.playerTiles.any{ it.entity != null})
			{
				beginLevel()
			}
		}
		beginTable.add(beginButton).expandX().center().height(30f).pad(2f)

		val buttonStack = Stack()
		buttonStack.add(beginTable)
		buttonStack.add(backTable)

		bottomArea.add(buttonStack).growX()
		bottomArea.row()
	}

	fun updateHeroWidgets()
	{
		for (widget in heroWidgets)
		{
			widget.alreadyUsed = false
			for (existing in level!!.playerTiles)
			{
				val ent = existing.entity ?: continue
				if (ent.loadData()!!.path == widget.entity.loadData()!!.path)
				{
					widget.alreadyUsed = true
					break
				}
			}
		}

		factionBonusTable.clear()

		class FactionCount(val faction: Faction, var count: Int)
		val playerFactions = ObjectMap<String, FactionCount>()
		for (tile in level!!.playerTiles)
		{
			val entity = tile.entity ?: continue
			val stats = entity.statsOrNull() ?: continue
			val factionData = stats.factionData ?: continue

			var faction = playerFactions[factionData.name]
			if (faction == null)
			{
				faction = FactionCount(factionData, 0)
				playerFactions[factionData.name] = faction
			}

			faction.count++
		}

		for (i in 0 until 5)
		{
			Global.data.lastSelectedHeroes[i] = level!!.playerTiles[i].entity?.name()?.name
		}

		for (faction in playerFactions)
		{
			for (i in 0 until faction.value.faction.buffs.size)
			{
				if (faction.value.count >= 2+i)
				{
					val stack = Stack()
					val widget = SpriteWidget(faction.value.faction.icon.copy(), 24f, 24f)
					stack.addTapToolTip(faction.value.faction.name + " ${i+2}:\n\n" + faction.value.faction.buffs[i].description)

					val table = Table()
					table.add(Label((i+2).toString(), Statics.skin, "small")).expand().bottom().right()

					stack.add(widget)
					stack.add(table)

					factionBonusTable.add(stack).size(24f).pad(5f)
				}
			}
		}
	}

	override fun modifyDelta(delta: Float): Float
	{
		val extraMult = if (level!!.selectingEntities || paused) 0f else 1f
		return delta * multipliers[multiplierIndex].multiplier * extraMult
	}

	fun createLevelEndTable(rewards: com.badlogic.gdx.utils.Array<Encounter.RewardDesc>): Table
	{
		val table = Table()

		val rewardsTable = Table()
		for (reward in rewards)
		{
			rewardsTable.add(reward.tile).size(64f).pad(10f)
		}
		table.add(rewardsTable).growX()
		table.row()

		table.add(Seperator(Statics.skin, false)).growX().pad(5f)
		table.row()

		val battleResultsTable = Table()

		val titleRow = Table()
		titleRow.add(Table()).width(32f)
		titleRow.add(Label("Damage", Statics.skin).align(Align.center)).width(Value.percentWidth(0.3f, battleResultsTable)).center()
		titleRow.add(Label("Healing", Statics.skin).align(Align.center)).width(Value.percentWidth(0.3f, battleResultsTable)).center()
		titleRow.add(Label("Hp Lost", Statics.skin).align(Align.center)).width(Value.percentWidth(0.3f, battleResultsTable)).center()

		battleResultsTable.add(titleRow).growX().pad(10f, 0f, 10f, 0f)
		battleResultsTable.row()

		var bright = true

		fun buildHeroRows(entities: com.badlogic.gdx.utils.Array<Entity>, colour: Colour)
		{
			var totalDamage = 0f
			var totalHealing = 0f
			var totalHpLost = 0f

			for (entity in entities)
			{
				totalDamage += entity.stats().damageDealt
				totalHealing += entity.stats().healing
				totalHpLost += entity.stats().totalHpLost
			}

			for (entity in entities)
			{
				val heroTexure = entity.directionalSprite()!!.directionalSprite.getSprite("idle", DirectionalSprite.VDir.DOWN, DirectionalSprite.HDir.RIGHT).textures[0]

				val rowTable = Table()
				if (bright)
				{
					rowTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
				}
				bright = !bright

				fun addNumberAndBar(num: Float, total: Float)
				{
					val percent = if (total > 0f) num / total else 0f

					val table = Table()

					table.add(Label(num.toInt().prettyPrint(), Statics.skin, "small"))
					table.row()
					table.add(PercentageBarWidget(percent, colour, 6)).growX().height(7f)

					rowTable.add(table).width(Value.percentWidth(0.3f, battleResultsTable)).center().expandY()
				}

				rowTable.add(SpriteWidget(Sprite(heroTexure), 24f, 24f)).size(26f)
				addNumberAndBar(entity.stats().damageDealt, totalDamage)
				addNumberAndBar(entity.stats().healing, totalHealing)
				addNumberAndBar(entity.stats().totalHpLost, totalHpLost)

				battleResultsTable.add(rowTable).growX()
				battleResultsTable.row()
			}
		}

		buildHeroRows(level!!.playerTiles.mapNotNull { it.entity }.toGdxArray(), Colour.GREEN)

		battleResultsTable.add(Table()).pad(20f)
		battleResultsTable.row()

		bright = true
		buildHeroRows(level!!.enemyTiles.mapNotNull { it.entity }.toGdxArray(), Colour.RED)

		table.add(battleResultsTable).grow()
		table.row()

		return table
	}

	override fun doRender(delta: Float)
	{
		Global.engine.update(delta)

		if (!levelComplete && !level!!.selectingEntities)
		{
			val winningFaction = level!!.isComplete()

			if (winningFaction != null)
			{
				val table = FullscreenTable(0.9f)
				table.touchable = Touchable.disabled
				table.color.a = 0f

				val clickAction: () -> Unit
				if (winningFaction == "1")
				{
					val label = Label("Victory", Statics.skin, "title")
					table.add(label).expandX().center().padTop(50f)
					table.row()

					table.add(Seperator(Statics.skin, false)).growX().pad(5f)
					table.row()

					val currentEncounter = zone!!.encounters[Global.data.currentZoneProgression]
					val rewards = currentEncounter.generateRewards(true)

					val rewardsTable = createLevelEndTable(rewards)
					table.add(rewardsTable).grow()

					clickAction = {

						val zone = zone!!
						Global.data.currentZoneProgression++
						zone.updateFlags()

						val targetScreen = Statics.game.getTypedScreen<ZoneScreen>()!!

						for (reward in rewards)
						{
							val src = table.localToStageCoordinates(Vector2(table.width / 2f, table.height / 2f))
							reward.func(src, targetScreen.stage, targetScreen.gameDataBar, targetScreen.navigationBar)
						}

						table.remove()

						targetScreen.swapTo()


					}
				}
				else
				{
					val label = Label("Defeat", Statics.skin, "title")
					table.add(label).expandX().center().padTop(50f)
					table.row()

					table.add(Seperator(Statics.skin, false)).growX().pad(5f)
					table.row()

					val currentEncounter = zone!!.encounters[Global.data.currentZoneProgression]
					val rewards = currentEncounter.generateRewards(false)

					val rewardsTable = createLevelEndTable(rewards)
					table.add(rewardsTable).grow()

					clickAction = {

						val targetScreen = Statics.game.getTypedScreen<ZoneScreen>()!!

						for (reward in rewards)
						{
							val src = table.localToStageCoordinates(Vector2(table.width / 2f, table.height / 2f))
							reward.func(src, targetScreen.stage, targetScreen.gameDataBar, targetScreen.navigationBar)
						}

						table.remove()

						targetScreen.swapTo()
					}
				}

				val showAction =
					alpha(0f) then
						delay(2f) then
						fadeIn(0.5f) then
						lambda {
							table.touchable = Touchable.enabled
							table.addClickListener(clickAction)
						}
				table.addAction(showAction)

				levelComplete = true
			}
		}

		if (!Statics.release)
		{
			systemUpdateAccumulator += delta
			if (systemUpdateAccumulator > 0.5f)
			{
				systemUpdateAccumulator = 0f
				var totalTime = 0f

				for (i in 0 until systemTimes.size)
				{
					val system = Global.engine.getSystem(systemList[i].java)
					val time = system.processDuration
					val perc = (time / frameDuration) * 100f
					systemTimes[i].time = perc
					systemTimes[i].entities = if (system.family != null) system.entities.size().toString() else "-"

					totalTime += perc
				}

				totalSystemTime = totalTime
			}
		}

		batch.begin()

		if (drawSystemTime)
		{
			val x = 20f
			var y = Statics.resolution.y - 40f

			font.draw(batch, "System Debug: Total time usage - $totalSystemTime%", x, y)
			y -= 20f

			for (pair in systemTimes.sortedByDescending { it.time })
			{
				font.draw(batch, pair.name + " (" + pair.entities + ")", x, y)
				font.draw(batch, " - ${pair.time}%", x + 15 * 10, y)
				y -= 20f
			}
		}

		batch.end()
	}

	override fun show()
	{
		super.show()

		for (system in Global.engine.systems)
		{
			if (system is AbstractSystem)
			{
				system.registerDebugCommands(debugConsole)
			}
		}
	}

	override fun hide()
	{
		super.hide()

		for (system in Global.engine.systems)
		{
			if (system is AbstractSystem)
			{
				system.unregisterDebugCommands(debugConsole)
			}
		}
	}
}