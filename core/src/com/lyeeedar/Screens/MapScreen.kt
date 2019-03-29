package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Components.loaddata
import com.lyeeedar.Components.name
import com.lyeeedar.Components.pos
import com.lyeeedar.Game.Level
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.AbstractSystem
import com.lyeeedar.Systems.systemList
import com.lyeeedar.UI.*
import com.lyeeedar.Util.Colour

class MapScreen : AbstractScreen()
{
	lateinit var batch: SpriteBatch

	var level: Level? = null

	var timeMultiplier = 1f

	data class SystemData(val name: String, var time: Float, var entities: String)
	val systemTimes: Array<SystemData> by lazy { Array<SystemData>(systemList.size) { i -> SystemData(systemList[i].java.simpleName.replace("System", ""), 0f, "--") } }
	var totalSystemTime: Float = 0f
	var drawSystemTime = false

	var clickedTile: Tile? = null
	var selectedEntity: Entity? = null
	var selectedComponent: IDebugCommandProvider? = null

	var systemUpdateAccumulator = 0f

	val bottomArea = Table()
	val heroesTable = Table()
	val heroWidgets = com.badlogic.gdx.utils.Array<HeroSelectionWidget>()

	override fun create()
	{
		batch = SpriteBatch()

		level = Level.load("Levels/Test")
		Global.changeLevel(level!!, Colour.BLACK)

		//val collisionGrid = Array2D<Boolean>(map.width, map.height) { x,y -> map.grid[x,y].type == TileType.WALL }
		//Global.collisionGrid = collisionGrid

		val mapWidget = RenderSystemWidget()

		mainTable.background = TiledDrawable(TextureRegionDrawable(level!!.theme.backgroundTile)).tint(level!!.ambient.color())
		mainTable.add(mapWidget).grow().height(Value.percentHeight(0.7f, mainTable))
		mainTable.row()
		mainTable.add(bottomArea).grow().height(Value.percentHeight(0.3f, mainTable))

		debugConsole.register("TimeMultiplier", "'TimeMultiplier speed' to enable, 'TimeMultiplier false' to disable", fun(args, console): Boolean {
			if (args[0] == "false")
			{
				timeMultiplier = 1f
				return true
			}
			else
			{
				try
				{
					val speed = args[0].toFloat()
					timeMultiplier = speed

					return true
				}
				catch (ex: Exception)
				{
					console.error(ex.message!!)
					return false
				}
			}
		})

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
					console.write("Selected entity: " + (selectedEntity!!.name()?.name ?: selectedEntity.toString()))
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

		selectEntities()
	}

	fun beginLevel()
	{
		bottomArea.clear()

		val entityTable = Table()
		entityTable.defaults().uniform().pad(1f)

		for (ent in level!!.playerTiles)
		{
			val widget = EntityWidget(ent.entity!!)
			entityTable.add(widget).grow()
		}

		bottomArea.add(entityTable).growX().height(75f).expandY().bottom()

		level!!.selectingEntities = false
	}

	fun selectEntities()
	{
		level!!.selectingEntities = true

		bottomArea.clear()

		val beginButton = TextButton("Begin", Global.skin)
		beginButton.addClickListener { beginLevel() }
		bottomArea.add(beginButton).expandX().center()
		bottomArea.row()


		heroesTable.defaults().uniform().pad(1f)
		val scroll = ScrollPane(heroesTable)
		scroll.setFadeScrollBars(false)
		scroll.setScrollingDisabled(true, false)
		scroll.setForceScroll(false, true)

		bottomArea.add(scroll).grow()

		heroWidgets.clear()
		heroesTable.clear()

		var x = 0
		for (hero in Global.data.heroPool)
		{
			val heroWidget = HeroSelectionWidget(hero)
			for (existing in level!!.playerTiles)
			{
				val ent = existing.entity ?: continue
				if (ent.loaddata()!!.path == hero.loaddata()!!.path)
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
							existing.entity = hero
							hero!!.pos().tile = existing.tile
							hero!!.pos().addToTile(hero)
							Global.engine.addEntity(hero)
							break
						}
					}
					updateHeroWidgets()
				}
			}

			heroWidgets.add(heroWidget)
			heroesTable.add(heroWidget).size(75f)
			x++
			if (x == 5)
			{
				x = 0
				heroesTable.row()
			}
		}
	}

	fun updateHeroWidgets()
	{
		for (widget in heroWidgets)
		{
			widget.alreadyUsed = false
			for (existing in level!!.playerTiles)
			{
				val ent = existing.entity ?: continue
				if (ent.loaddata()!!.path == widget.entity.loaddata()!!.path)
				{
					widget.alreadyUsed = true
					break
				}
			}
		}
	}

	override fun doRender(delta: Float)
	{
		val extraMult = if (level!!.selectingEntities) 0f else 1f
		Global.engine.update(delta * timeMultiplier * extraMult)

		if (!Global.release)
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
			var y = Global.resolution.y - 40f

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