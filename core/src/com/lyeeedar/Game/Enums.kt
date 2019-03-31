package com.lyeeedar

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.*

// ----------------------------------------------------------------------
enum class BlendMode constructor(val src: Int, val dst: Int)
{
	MULTIPLICATIVE(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
	ADDITIVE(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
}

// ----------------------------------------------------------------------
enum class Ascension(val multiplier: Float, val colour: Colour)
{
	MUNDANE(1f, Colour(152, 127, 95, 255, true)),
	EXCEPTIONAL(1.2f, Colour(107, 193, 101, 255, true)),
	EXTRAORDINARY(1.4f, Colour(79, 185, 243, 255, true)),
	FABLED(1.6f, Colour(137, 20, 223, 255, true)),
	LEGENDARY(2.0f, Colour(255, 219, 0, 255, true)),
	MYTHICAL(2.5f, Colour(186, 0, 39, 255, true)),
	DIVINE(3.0f, Colour(255, 221, 249, 255, true));

	companion object
	{
		val Values = Ascension.values()
	}
}

// ----------------------------------------------------------------------
enum class Rarity
{
	COMMON,
	UNCOMMON,
	RARE;

	companion object
	{
		val Values = Rarity.values()
	}
}

// ----------------------------------------------------------------------
enum class SpawnWeight
{
	ANY,
	START,
	STARTMIDDLE,
	MIDDLE,
	MIDDLEEND,
	END;

	val subWeights = Array<SpawnWeight>()

	companion object
	{
		val Values = SpawnWeight.values()

		init
		{
			ANY.subWeights.add(START, MIDDLE, END)

			START.subWeights.add(START)

			STARTMIDDLE.subWeights.add(START, MIDDLE)

			MIDDLE.subWeights.add(MIDDLE)

			MIDDLEEND.subWeights.add(MIDDLE, END)

			END.subWeights.add(END)
		}
	}
}

// ----------------------------------------------------------------------
enum class Direction private constructor(val x: Int, val y: Int, val identifier: String)
{
	CENTER(0, 0, "C"),
	NORTH(0, 1, "N"),
	SOUTH(0, -1, "S"),
	EAST(1, 0, "E"),
	WEST(-1, 0, "W"),
	NORTHEAST(1, 1, "NE"),
	NORTHWEST(-1, 1, "NW"),
	SOUTHEAST(1, -1, "SE"),
	SOUTHWEST(-1, -1, "SW");

	val angle: Float // In degrees
	lateinit var clockwise: Direction
		private set
	lateinit var anticlockwise: Direction
		private set
	var isCardinal = false
		private set

	val cardinalClockwise: Direction
			get() = clockwise.clockwise
	val cardinalAnticlockwise: Direction
			get() = anticlockwise.anticlockwise

	init
	{
		angle = vectorToAngle(x.toFloat(), y.toFloat())
	}

	val opposite: Direction
		get() = getDirection(x * -1, y * -1)

	companion object
	{
		init
		{
			// Setup neighbours
			CENTER.clockwise = CENTER
			CENTER.anticlockwise = CENTER

			NORTH.anticlockwise = NORTHWEST
			NORTH.clockwise = NORTHEAST

			NORTHEAST.anticlockwise = NORTH
			NORTHEAST.clockwise = EAST

			EAST.anticlockwise = NORTHEAST
			EAST.clockwise = SOUTHEAST

			SOUTHEAST.anticlockwise = EAST
			SOUTHEAST.clockwise = SOUTH

			SOUTH.anticlockwise = SOUTHEAST
			SOUTH.clockwise = SOUTHWEST

			SOUTHWEST.anticlockwise = SOUTH
			SOUTHWEST.clockwise = WEST

			WEST.anticlockwise = SOUTHWEST
			WEST.clockwise = NORTHWEST

			NORTHWEST.anticlockwise = WEST
			NORTHWEST.clockwise = NORTH

			// Setup is cardinal
			NORTH.isCardinal = true
			SOUTH.isCardinal = true
			EAST.isCardinal = true
			WEST.isCardinal = true
		}

		val CardinalValues = arrayOf(NORTH, EAST, SOUTH, WEST)
		val CardinalValuesAndCenter = arrayOf(NORTH, EAST, SOUTH, WEST, CENTER)
		val DiagonalValues = arrayOf(NORTHEAST, NORTHWEST, SOUTHWEST, SOUTHWEST)
		val Values = Direction.values()

		fun getDirection(point: Point): Direction
		{
			return getDirection(point.x, point.y)
		}

		fun getDirection(path: kotlin.Array<Vector2>): Direction
		{
			val x = path.last().x - path.first().x
			val y = path.last().y - path.first().y

			return getDirection(x.toInt(), y.toInt())
		}

		fun getDirection(dir: FloatArray): Direction
		{
			val x = if (dir[0] < 0) -1 else if (dir[0] > 0) 1 else 0
			val y = if (dir[1] < 0) -1 else if (dir[1] > 0) 1 else 0

			return getDirection(x, y)
		}

		fun getDirection(dir: IntArray): Direction
		{
			return getDirection(dir[0], dir[1])
		}

		fun getDirection(dx: Int, dy: Int): Direction
		{
			var dx = dx
			var dy = dy
			dx = MathUtils.clamp(dx, -1, 1)
			dy = MathUtils.clamp(dy, -1, 1)

			var d = CENTER

			for (dir in Values)
			{
				if (dir.x == dx && dir.y == dy)
				{
					d = dir
					break
				}
			}

			return d
		}

		fun getCardinalDirection(p1: Point, p2: Point): Direction
		{
			return getCardinalDirection(p1.x - p2.x, p1.y - p2.y)
		}

		fun getCardinalDirection(dx: Int, dy: Int): Direction
		{
			if (dx == 0 && dy == 0)
			{
				return CENTER
			}

			if (Math.abs(dx) > Math.abs(dy))
			{
				if (dx < 0)
				{
					return WEST
				} else
				{
					return EAST
				}
			} else
			{
				if (dy < 0)
				{
					return SOUTH
				} else
				{
					return NORTH
				}
			}
		}

		fun getDirection(p1: Point, p2: Point): Direction
		{
			return getDirection(p2.x - p1.x, p2.y - p1.y)
		}

		fun buildCone(dir: Direction, start: Point, range: Int): Array<Point>
		{
			val hitTiles = Array<Point>()

			val anticlockwise = dir.anticlockwise
			val clockwise = dir.clockwise

			val acwOffset = Point.obtain().set(dir.x - anticlockwise.x, dir.y - anticlockwise.y)
			val cwOffset = Point.obtain().set(dir.x - clockwise.x, dir.y - clockwise.y)

			hitTiles.add(Point.obtain().set(start.x + anticlockwise.x, start.y + anticlockwise.y))

			hitTiles.add(Point.obtain().set(start.x + dir.x, start.y + dir.y))

			hitTiles.add(Point.obtain().set(start.x + clockwise.x, start.y + clockwise.y))

			for (i in 2..range)
			{
				val acx = start.x + anticlockwise.x * i
				val acy = start.y + anticlockwise.y * i

				val nx = start.x + dir.x * i
				val ny = start.y + dir.y * i

				val cx = start.x + clockwise.x * i
				val cy = start.y + clockwise.y * i

				// add base tiles
				hitTiles.add(Point.obtain().set(acx, acy))
				hitTiles.add(Point.obtain().set(nx, ny))
				hitTiles.add(Point.obtain().set(cx, cy))

				// add anticlockwise - mid
				for (ii in 1..range)
				{
					val px = acx + acwOffset.x * ii
					val py = acy + acwOffset.y * ii

					hitTiles.add(Point.obtain().set(px, py))
				}

				// add mid - clockwise
				for (ii in 1..range)
				{
					val px = cx + cwOffset.x * ii
					val py = cy + cwOffset.y * ii

					hitTiles.add(Point.obtain().set(px, py))
				}
			}

			acwOffset.free()
			cwOffset.free()

			return hitTiles
		}
	}
}

// ----------------------------------------------------------------------
enum class Statistic private constructor(val min: Float, val max: Float, val modifiersAreAdded: Boolean, val niceName: String, val tooltip: String)
{
	MAXHP(1f, Float.MAX_VALUE, false, "Health", "The amount of damage you can take before dieing"),
	POWER(1f, Float.MAX_VALUE, false, "Power", "The damage of your attacks and the effectiveness of your abilities"),
	DR(0f, 1f, true, "Damage Resistance", "Your resistance to damage"),
	CRITCHANCE(0f, 1f, true, "Critical Chance", "The chance to deal a critical hit anytime you deal damage"),
	CRITDAMAGE(1f, Float.MAX_VALUE, true, "Critical Damage", "The multiplier to your damage when you deal a critical hit"),
	REGENERATION(0f, 1f, true, "Regeneration", "The percentage of your max health you gain each turn"),
	HASTE(0f, 1f, true, "Haste", "How fast you act"),
	LIFESTEAL(0f, 1f, true, "Life Steal", "The portion of your damage dealt you absorb as life"),
	BUFFDURATION(0f, Float.MAX_VALUE, true, "Buff Duration", "The bonus to the duration of buffs you create."),
	BUFFPOWER(0f, Float.MAX_VALUE, true, "Buff Power", "The bonus to the power of buffs you create."),
	DEBUFFDURATION(0f, Float.MAX_VALUE, true, "Debuff Duration", "The bonus to the duration of debuffs you create."),
	DEBUFFPOWER(0f, Float.MAX_VALUE, true, "Debuff Power", "The bonus to the power of debuffs you create."),
	AEGIS(0f, 1f, true, "Aegis", "The chance to avoid completely block damage when hit."),
	ABILITYCOOLDOWN(0f, Float.MAX_VALUE, true, "Ability Cooldown", "The rate at which your abilities come off cooldown.");

	companion object
	{
		val Values = Statistic.values()

		fun parse(xmlData: XmlData, statistics: FastEnumMap<Statistic, Float>)
		{
			for (stat in Values)
			{
				var value = statistics[stat] ?: 0f
				value = xmlData.getFloat(stat.toString(), value)
				statistics[stat] = value
			}
		}
	}
}

// ----------------------------------------------------------------------
enum class EquipmentSlot
{
	HEAD,
	MAINHAND,
	OFFHAND,
	BODY;

	companion object
	{
		val Values = EquipmentSlot.values()
	}
}

// ----------------------------------------------------------------------
enum class SpaceSlot
{
	FLOOR,
	FLOORDETAIL,
	WALL,
	WALLDETAIL,
	BELOWENTITY,
	ENTITY,
	ABOVEENTITY,
	EFFECT,
	LIGHT;


	companion object
	{

		val Values = SpaceSlot.values()
		val BasicValues = arrayOf(FLOOR, FLOORDETAIL, WALL, WALLDETAIL)
		val EntityValues = arrayOf(BELOWENTITY, ENTITY, ABOVEENTITY)
	}
}