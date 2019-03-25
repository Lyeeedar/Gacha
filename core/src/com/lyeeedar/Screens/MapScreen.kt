package com.lyeeedar.Screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.lyeeedar.Components.name
import com.lyeeedar.Game.Level
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.AbstractSystem
import com.lyeeedar.Systems.systemList
import com.lyeeedar.UI.IDebugCommandProvider
import com.lyeeedar.UI.RenderSystemWidget
import com.lyeeedar.Util.Colour

class MapScreen : AbstractScreen()
{
	lateinit var batch: SpriteBatch

	var level: Level? = null

	var timeMultiplier = 0.5f

	data class SystemData(val name: String, var time: Float, var entities: String)
	val systemTimes: Array<SystemData> by lazy { Array<SystemData>(systemList.size) { i -> SystemData(systemList[i].java.simpleName.replace("System", ""), 0f, "--") } }
	var totalSystemTime: Float = 0f
	var drawSystemTime = false

	var clickedTile: Tile? = null
	var selectedEntity: Entity? = null
	var selectedComponent: IDebugCommandProvider? = null

	var systemUpdateAccumulator = 0f

	override fun create()
	{
		batch = SpriteBatch()

		level = Level.load("Levels/Test")
		Global.changeLevel(level!!, Colour.GOLD)

		//val collisionGrid = Array2D<Boolean>(map.width, map.height) { x,y -> map.grid[x,y].type == TileType.WALL }
		//Global.collisionGrid = collisionGrid

		val mapWidget = RenderSystemWidget()

		mainTable.debug()
		mainTable.add(mapWidget).grow()

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
	}

	override fun doRender(delta: Float)
	{
		Global.engine.update(delta * timeMultiplier)

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