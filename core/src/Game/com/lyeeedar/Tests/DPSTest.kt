package com.lyeeedar.Tests

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Level
import com.lyeeedar.Game.Theme
import com.lyeeedar.Game.Tile
import com.lyeeedar.Global
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData

class DPSTest
{
	fun testEntity(path: String): Float
	{
		val theme = Theme.load("GrassyKnoll")
		val grid = Array2D<Tile>(10, 10) { x, y -> Tile(x, y) }

		val level = Level(grid, theme)
		for (tile in grid)
		{
			tile.level = level
		}

		val attackerEntity = EntityLoader.load(path, Global.resolveInstant)
		val defenderEntity = EntityLoader.load("Factions/DPSTest", Global.resolveInstant)

		attackerEntity.pos().tile = grid[2, 5]
		attackerEntity.pos().addToTile(attackerEntity)
		attackerEntity.stats().faction = "1"

		defenderEntity.pos().tile = grid[8, 5]
		defenderEntity.pos().addToTile(defenderEntity)
		defenderEntity.stats().faction = "2"
		val startHp = defenderEntity.stats().hp

		Global.changeLevel(level)
		level.begin()

		Global.resolveInstant = true
		for (i in 0 until 1000)
		{
			Global.engine.update(1f)
			Future.update(1f)
		}
		Global.resolveInstant = false

		val damageDealt = startHp - defenderEntity.stats().hp
		val dps = (damageDealt) / 1000f
		return dps
	}

	fun runDPSTest()
	{
		class EntityEntry(val path: String, val dps: Float)
		val entries = Array<EntityEntry>()
		for (entity in XmlData.enumeratePaths("Factions", "Entity"))
		{
			val dps = testEntity(entity)
			entries.add(EntityEntry(entity, dps))
		}

		val ordered = entries.sortedByDescending { it.dps }
		for (entry in ordered)
		{
			println(entry.path + ": 			" + entry.dps)
		}
	}
}