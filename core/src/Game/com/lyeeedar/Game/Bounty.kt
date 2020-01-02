package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Ascension
import com.lyeeedar.Components.name
import com.lyeeedar.Global
import com.lyeeedar.MainGame
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.actors.alpha
import ktx.actors.then
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

		val ascensionLabel = Label(ascension.name.neaten(), Statics.skin, "title")
		ascensionLabel.color = ascension.colour.color()

		table.add(ascensionLabel).pad(5f)
		table.row()

		table.add(getTile()).pad(10f)
		table.row()

		val titleLabel = Label(title, Statics.skin, "cardtitle").wrap()
		titleLabel.align(Align.center)
		table.add(titleLabel).growX().pad(10f)
		table.row()

		val durationLabel = Label(durationMillis.millisToPrettyTime(showSeconds = false), Statics.skin, "card")
		table.add(durationLabel).pad(10f)
		table.row()

		return table
	}

	abstract fun collect(source: Actor, gameDataBar: GameDataBar, navigationBar: NavigationBar)

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
		goldAmountTable.add(Label(amount.prettyPrint(), Statics.skin, "small")).padBottom(5f)

		goldTile.addTable(goldAmountTable).expandY().growX().bottom()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), tileSize, tileSize))

		val table = Table()
		table.add(goldTile).grow()

		return table
	}

	override fun collect(source: Actor, gameDataBar: GameDataBar, navigationBar: NavigationBar)
	{
		Future.call({ Global.data.gold += amount }, 1f)

		var delay = 0f
		for (i in 0 until 15)
		{
			val src = source.localToStageCoordinates(Vector2(source.width / 2f, source.height / 2f))

			val dstTable = gameDataBar.goldTable
			val dst = dstTable.localToStageCoordinates(Vector2())

			val widget = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold"), 16f, 16f)
			widget.setSize(16f, 16f)
			widget.setPosition(src.x, src.y)
			widget.toFront()
			widget.alpha = 0f

			val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
			sparkleParticle.color = Color.GOLD
			sparkleParticle.setSize(dstTable.height, dstTable.height)
			sparkleParticle.setPosition(dst.x, dst.y)

			val sequence =
				alpha(0f) then
				Actions.delay(delay) then
					alpha(1f) then
				Actions.parallel(
					mote(src, dst, 1f, Interpolation.exp5, false),
					Actions.scaleTo(0.7f, 0.7f, 1f),
					Actions.delay(0.8f) then lambda { Statics.stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
					Actions.removeActor()
			widget.addAction(sequence)

			Statics.stage.addActor(widget)

			delay += 0.01f
		}
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
			"Gather at",
			"Delve into",
			"Examine",
			"Hunt in",
			"Prospect",
			"Scout",
			"Quest at",
			"Quest in",
			"Adventure at",
			"Adventure in",
			"Journey to",
			"Voyage to",
			"Assault",
			"Desecrate",
			"Ransack",
			"Despoil",
			"Sack"
		)
		val action = actions.random(ran)

		// pick location
		val locationName = FakeLanguageGen.FANTASY_NAME.word(RNG(ran), true)

		val locationTypes = arrayOf(
			"Ruins",
			"Remains",
			"Wreckage",
			"Wreck",
			"Crypts",
			"Tombs",
			"Catacombs",
			"Dungeon",
			"Mausoleum",
			"Sepulcher",
			"Vault",
			"Shrine",
			"Palace",
			"Mansion",
			"Castle",
			"Manor",
			"Chateau",
			"Temple",
			"Chapel",
			"Church",
			"Pagoda",
			"Sanctuary",
			"Grave",
			"Forest",
			"Caves",
			"Cave",
			"Caverns",
			"Wood",
			"Thicket",
			"Woodland",
			"Jungle",
			"Copse",
			"Weald",
			"Grove"
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
		amountTable.add(Label(amount.prettyPrint(), Statics.skin, "small")).padBottom(5f)

		tile.addTable(amountTable).expandY().growX().bottom()
		tile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), tileSize, tileSize))

		val table = Table()
		table.add(tile).grow()

		return table
	}

	override fun collect(source: Actor, gameDataBar: GameDataBar, navigationBar: NavigationBar)
	{
		Future.call({ Global.data.experience += amount }, 1f)

		var delay = 0f
		for (i in 0 until 15)
		{
			val src = source.localToStageCoordinates(Vector2(source.width / 2f, source.height / 2f))

			val dstTable = gameDataBar.expTable
			val dst = dstTable.localToStageCoordinates(Vector2())

			val widget = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/crystal_sky"), 16f, 16f)
			widget.setSize(16f, 16f)
			widget.setPosition(src.x, src.y)
			widget.toFront()
			widget.alpha = 0f

			val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
			sparkleParticle.color = Color.CYAN
			sparkleParticle.setSize(dstTable.height, dstTable.height)
			sparkleParticle.setPosition(dst.x, dst.y)

			val sequence =
				alpha(0f) then
					Actions.delay(delay) then
					alpha(1f) then
					Actions.parallel(
						mote(src, dst, 1f, Interpolation.exp5, false),
						Actions.scaleTo(0.7f, 0.7f, 1f),
						Actions.delay(0.8f) then lambda { Statics.stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
					Actions.removeActor()
			widget.addAction(sequence)

			Statics.stage.addActor(widget)

			delay += 0.01f
		}
	}

	override fun getClassName(): String = "EXP"

	override fun doGenerate(ran: LightRNG)
	{
		val level = Global.data.getCurrentLevel()
		val expRequired = Global.getExperienceForLevel(level)

		val reward = expRequired + (expRequired * ascension.multiplier * 1.5f).toInt()
		amount = reward

		// generate title

		// pick action
		val actions = arrayOf(
			"Study with",
			"Debate with",
			"Exercise with",
			"Inspect",
			"Survey",
			"Inquire about",
			"Train with",
			"Spar with",
			"Listen to",
			"Observe",
			"Fight against",
			"Attend a lecture by",
			"Take a class by",
			"Get instructed by",
			"Get taught by",
			"Challenge",
			"Clash with",
			"Protect",
			"Meet",
			"Assault",
			"Brawl with",
			"Duel",
			"Grapple with",
			"Joust with",
			"Scuffle with",
			"Scrap with",
			"Wrestle with",
			"Exchange blows with"
							 )
		val action = actions.random(ran)

		// pick person
		val personName = FakeLanguageGen.FANTASY_NAME.word(RNG(ran), true)

		val personTitles = arrayOf(
			"Captain",
			"Boss",
			"Commander",
			"Officer",
			"Chieftan",
			"Mistress",
			"General",
			"Master",
			"Mage",
			"Wizard",
			"Scholar",
			"Knight",
			"Fighter",
			"Warrior",
			"Lieutenant",
			"Astrologer",
			"Conjurer",
			"Diviner",
			"Enchanter",
			"Magus",
			"Necromancer",
			"Occultist",
			"Shaman",
			"Sorcerer",
			"Warlock",
			"Witch",
			"Cavalier",
			"Champion",
			"Chevalier",
			"Paladin",
			"Warpriest",
			"Cleric",
			"Templar",
			"Hero",
			"Lord"
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

	override fun collect(source: Actor, gameDataBar: GameDataBar, navigationBar: NavigationBar)
	{
		Global.data.equipment.add(equipment)

		val src = source.localToStageCoordinates(Vector2(source.width / 2f, source.height / 2f))

		val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
		val dst = dstTable.localToStageCoordinates(Vector2())

		val widget = MaskedTexture(equipment.fullIcon)
		widget.setSize(48f, 48f)
		widget.setPosition(src.x, src.y)
		widget.toFront()

		val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
		sparkleParticle.color = Color.WHITE.cpy().lerp(equipment.ascension.colour.color(), 0.3f)
		sparkleParticle.setSize(dstTable.height, dstTable.height)
		sparkleParticle.setPosition(dstTable.x + dstTable.width*0.5f - dstTable.height * 0.5f, dstTable.y)

		val sequence =
				Actions.parallel(
					mote(src, dst, 1f, Interpolation.exp5, false),
					Actions.scaleTo(0.7f, 0.7f, 1f),
					Actions.delay(0.9f) then lambda { Statics.stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
				Actions.removeActor()
		widget.addAction(sequence)

		Statics.stage.addActor(widget)
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
			"Nab",
			"Appropriate",
			"Pinch",
			"Purloin",
			"Confiscate",
			"Rob",
			"Pilfer",
			"Acquire",
			"Obtain",
			"Discover",
			"Make",
			"Build",
			"Craft",
			"Forge",
			"Create",
			"Design",
			"Conceive",
			"Construct",
			"Invent",
			"Produce",
			"Shape",
			"Fabricate",
			"Assemble",
			"Manufacture",
			"Engineer"
							 )
		val action = actions.random(ran)

		val a = if (equipment.fullName[0].isVowel()) "an" else "a"

		title = "$action $a ${equipment.fullName}"
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

		return entityData.factionEntity.createTile(tileSize, Ascension.MUNDANE)
	}

	override fun collect(source: Actor, gameDataBar: GameDataBar, navigationBar: NavigationBar)
	{
		val entityData = Global.data.heroPool.first { it.factionEntity.entityPath == hero }
		entityData.ascensionShards += 1

		val src = source.localToStageCoordinates(Vector2(source.width / 2f, source.height / 2f))

		val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
		val dst = dstTable.localToStageCoordinates(Vector2())

		val widget = SpriteWidget(AssetManager.loadSprite("Particle/shard"), 16f, 16f)
		widget.color = Colour.WHITE.copy().lerp(entityData.factionEntity.rarity.colour, 0.7f).color()
		widget.setSize(16f, 16f)
		widget.setPosition(src.x, src.y)
		widget.toFront()

		val chosenDst = dst.cpy()
		chosenDst.x += dstTable.width * 0.5f * Random.random()
		chosenDst.y += dstTable.height * 0.5f * Random.random()

		val particle = AssetManager.loadParticleEffect("GetShard", timeMultiplier = 1.2f)
		particle.colour = Colour.WHITE.copy().lerp(entityData.factionEntity.rarity.colour, 0.7f)

		val shardParticle = ParticleEffectActor(particle.getParticleEffect(), true)
		shardParticle.setSize(dstTable.height, dstTable.height)
		shardParticle.setPosition(chosenDst.x - 16f, chosenDst.y - 16f)

		val sequence =
			Actions.parallel(
				mote(src, chosenDst, 1f + 0.02f, Interpolation.exp5, true),
				Actions.scaleTo(0.7f, 0.7f, 1f),
				Actions.delay(0.8f + 0.02f) then lambda { Statics.stage.addActor(shardParticle) } then Actions.fadeOut(0.1f)) then
				Actions.removeActor()
		widget.addAction(sequence)

		Statics.stage.addActor(widget)
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
		ascension = heroData.rarity.ascension

		val entityData = Global.data.heroPool.first { it.factionEntity == heroData }
		val entity = entityData.getEntity("1")
		val name = entity.name()!!.name

		// generate title

		// pick action
		val actions = arrayOf(
			"Recruit",
			"Train",
			"Enhance",
			"Strengthen",
			"Empower",
			"Improve",
			"Boost",
			"Advance",
			"Upgrade",
			"Augment",
			"Cultivate",
			"Develop",
			"Refine",
			"Increase",
			"Progress",
			"Promote",
			"Elevate",
			"Reinforce",
			"Heighten",
			"Magnify"
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