package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.name
import com.lyeeedar.Components.renderable
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTable
import com.lyeeedar.UI.align
import com.lyeeedar.UI.wrap
import com.lyeeedar.Util.*
import squidpony.FakeLanguageGen
import squidpony.squidmath.LightRNG
import squidpony.squidmath.RNG
import java.lang.System.currentTimeMillis

abstract class AbstractBounty
{
	val tileSize = 64f

	val hoursToMillis = 60 * 60 * 1000L

	var startTimeMillis: Long = 0L
	var durationMillis: Long = 0L
	lateinit var title: String
	var ascension: Ascension = Ascension.MUNDANE

	fun isComplete() = remaining() <= 0
	fun endTime() = startTimeMillis + durationMillis
	fun remaining() = endTime() - currentTimeMillis()

	abstract fun getTile(): Table
	abstract fun getClassName(): String

	fun createCardTable(): Table
	{
		val table = Table()

		val ascensionLabel = Label(ascension.name.neaten(), Global.skin, "title")
		ascensionLabel.color = ascension.colour.color()

		table.add(ascensionLabel).pad(5f)
		table.row()

		table.add(getTile()).pad(10f)
		table.row()

		val titleLabel = Label(title, Global.skin, "cardtitle").wrap()
		titleLabel.align(Align.center)
		table.add(titleLabel).growX().pad(10f)
		table.row()

		val durationLabel = Label(durationMillis.millisToPrettyTime(showSeconds = false), Global.skin, "card")
		table.add(durationLabel).pad(10f)
		table.row()

		return table
	}

	abstract fun collect()

	fun generate(ran: LightRNG)
	{
		ascension = Ascension.getWeightedAscension(ran)

		doGenerate(ran)

		durationMillis = ((1f + ascension.multiplier) * hoursToMillis).toLong()
	}
	abstract fun doGenerate(ran: LightRNG)

	fun save(xmlData: XmlData)
	{
		xmlData.set("Type", getClassName())
		xmlData.set("StartTime", startTimeMillis)
		xmlData.set("Duration", durationMillis)
		xmlData.set("Title", title)
		xmlData.set("Ascension", ascension.ordinal)

		doSave(xmlData)
	}
	abstract fun doSave(xmlData: XmlData)

	fun load(xmlData: XmlData)
	{
		val typename = xmlData.get("Type")
		if (typename != getClassName()) throw Exception("Bounty type ${getClassName()} does not match xml name $typename!")

		startTimeMillis = xmlData.getLong("StartTime")
		durationMillis = xmlData.getLong("Duration")
		title = xmlData.get("Title")
		ascension = Ascension.Values[xmlData.getInt("Ascension")]

		doLoad(xmlData)
	}
	abstract fun doLoad(xmlData: XmlData)

	companion object
	{
		fun load(xmlData: XmlData): AbstractBounty
		{
			val name = xmlData.get("Type")
			val bounty = create(name)

			bounty.load(xmlData)

			return bounty
		}

		fun generate(ran: LightRNG = Random.random): AbstractBounty
		{
			val types = arrayOf(
				"GOLD",
				"EXP",
				"EQUIPMENT",
				"HERO"
			)

			val bounty = create(types.random(ran))
			bounty.generate(ran)

			return bounty
		}

		fun create(name: String): AbstractBounty
		{
			return when (name.toUpperCase())
			{
				"GOLD" -> GoldBounty()
				"EXP" -> ExpBounty()
				"EQUIPMENT" -> EquipmentBounty()
				"HERO" -> HeroBounty()
				else -> throw Exception("Unknown bounty type $name!")
			}
		}
	}
}

class GoldBounty : AbstractBounty()
{
	var amount: Int = 0

	override fun getTile(): Table
	{
		val goldTile = Stack()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), tileSize, tileSize))
		goldTile.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), tileSize, tileSize))

		val goldAmountTable = Table()
		goldAmountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		goldAmountTable.add(Label(amount.prettyPrint(), Global.skin, "small")).padBottom(5f)

		goldTile.addTable(goldAmountTable).expandY().growX().bottom()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), tileSize, tileSize))

		val table = Table()
		table.add(goldTile).grow()

		return table
	}

	override fun collect()
	{
		Global.data.gold += amount
	}

	override fun getClassName(): String = "GOLD"

	override fun doGenerate(ran: LightRNG)
	{
		// start with 500 gold
		var gold = 500

		gold += (gold * ascension.multiplier * 1.5f).toInt()

		amount = gold

		// generate title

		// pick action
		val actions = arrayOf(
			"Raid",
			"Pillage",
			"Loot",
			"Explore",
			"Map",
			"Investigate",
			"Collect in",
			"Search",
			"Gather at"
		)
		val action = actions.random(ran)

		// pick location
		val locationName = FakeLanguageGen.FANTASY_NAME.word(RNG(ran), true)

		val locationTypes = arrayOf(
			"Ruins",
			"Crypts",
			"Shrine",
			"Palace",
			"Temple",
			"Grave",
			"Forest",
			"Caves",
			"Cave",
			"Wood"
		)
		val locationType = locationTypes.random(ran)

		val arrangementTypes = arrayOf(
			"$locationType of $locationName",
			"$locationName $locationType"
		)
		val arrangement = arrangementTypes.random(ran)

		title = "$action the $arrangement"
	}

	override fun doSave(xmlData: XmlData)
	{
		xmlData.set("Amount", amount)
	}

	override fun doLoad(xmlData: XmlData)
	{
		amount = xmlData.getInt("Amount")
	}
}

class ExpBounty : AbstractBounty()
{
	var amount: Int = 0

	override fun getTile(): Table
	{
		val tile = Stack()
		tile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), tileSize, tileSize))
		tile.add(SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_blue"), tileSize, tileSize))

		val amountTable = Table()
		amountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		amountTable.add(Label(amount.prettyPrint(), Global.skin, "small")).padBottom(5f)

		tile.addTable(amountTable).expandY().growX().bottom()
		tile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), tileSize, tileSize))

		val table = Table()
		table.add(tile).grow()

		return table
	}

	override fun collect()
	{
		Global.data.experience += amount
	}

	override fun getClassName(): String = "EXP"

	override fun doGenerate(ran: LightRNG)
	{
		val level = Global.data.getCurrentLevel()
		val expRequired = (100 * Math.pow(1.2, level.toDouble())).toInt()

		val reward = expRequired + (expRequired * ascension.multiplier * 1.5f).toInt()
		amount = reward

		// generate title

		// pick action
		val actions = arrayOf(
			"Study with",
			"Train with",
			"Spar with",
			"Listen to",
			"Observe",
			"Fight against",
			"Attend a lecture by",
			"Take a class by"
							 )
		val action = actions.random(ran)

		// pick person
		val personName = FakeLanguageGen.FANTASY_NAME.word(RNG(ran), true)

		val personTitles = arrayOf(
			"Captain",
			"General",
			"Master",
			"Mage",
			"Wizard",
			"Scholar",
			"Knight",
			"Fighter",
			"Warrior",
			"Lieutenant"
								   )
		val personTitle = personTitles.random(ran)

		title = "$action $personTitle $personName"
	}

	override fun doSave(xmlData: XmlData)
	{
		xmlData.set("Amount", amount)
	}

	override fun doLoad(xmlData: XmlData)
	{
		amount = xmlData.getInt("Amount")
	}
}

class EquipmentBounty : AbstractBounty()
{
	lateinit var equipment: Equipment

	override fun getTile(): Table
	{
		return equipment.createTile(tileSize)
	}

	override fun collect()
	{
		Global.data.equipment.add(equipment)
	}

	override fun getClassName(): String = "EQUIPMENT"

	override fun doGenerate(ran: LightRNG)
	{
		val level = Global.data.getCurrentLevel()
		equipment = EquipmentCreator.createRandom(level, ran, ascension = ascension)

		// generate title

		// pick action
		val actions = arrayOf(
			"Steal",
			"Pilfer",
			"Acquire",
			"Obtain",
			"Discover",
			"Make",
			"Build",
			"Craft",
			"Forge",
			"Create",
			"Design"
							 )
		val action = actions.random(ran)

		title = "$action a ${equipment.fullName}"
	}

	override fun doSave(xmlData: XmlData)
	{
		val equipEl = xmlData.addChild("Equipment")
		equipment.save(equipEl)
	}

	override fun doLoad(xmlData: XmlData)
	{
		equipment = Equipment.load(xmlData.getChildByName("Equipment")!!)
	}
}

class HeroBounty : AbstractBounty()
{
	lateinit var hero: String

	override fun getTile(): Table
	{
		val entityData = Global.data.heroPool.first { it.factionEntity.entityPath == hero }
		val entity = entityData.getEntity("1")
		val originalSprite = entity.renderable().renderable as Sprite
		val sprite = Sprite(originalSprite.textures[0])

		val tile = Stack()
		tile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), tileSize, tileSize))
		tile.add(SpriteWidget(sprite, tileSize, tileSize))
		tile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), tileSize, tileSize))

		val table = Table()
		table.add(tile).grow()

		return table
	}

	override fun collect()
	{
		val entityData = Global.data.heroPool.first { it.factionEntity.entityPath == hero }
		entityData.ascensionShards += 1
	}

	override fun getClassName(): String = "HERO"

	override fun doGenerate(ran: LightRNG)
	{
		val possibleDrops = Array<FactionEntity>()
		for (faction in Global.data.unlockedFactions)
		{
			for (hero in faction.heroes)
			{
				if (Global.data.heroPool.any{ it.factionEntity == hero})
				{
					for (i in 0 until hero.rarity.dropRate)
					{
						possibleDrops.add(hero)
					}
				}
			}
		}

		val heroData = possibleDrops.random(ran)
		hero = heroData.entityPath

		val entityData = Global.data.heroPool.first { it.factionEntity == heroData }
		val entity = entityData.getEntity("1")
		val name = entity.name().name

		// generate title

		// pick action
		val actions = arrayOf(
			"Recruit",
			"Train",
			"Enhance",
			"Strengthen",
			"Empower",
			"Enchant"
							 )
		val action = actions.random(ran)

		title = "$action $name"
	}

	override fun doSave(xmlData: XmlData)
	{
		xmlData.set("Hero", hero)
	}

	override fun doLoad(xmlData: XmlData)
	{
		hero = xmlData.get("Hero")
	}
}