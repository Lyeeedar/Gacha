package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.*
import com.lyeeedar.Components.*
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.UI.*
import com.lyeeedar.Util.*
import ktx.actors.alpha
import ktx.actors.then
import ktx.collections.set
import ktx.collections.toGdxArray

class Zone(val zoneIndex: Int)
{
	val ran = Random.obtainTS(20181201L + zoneIndex)

	val factions = Array<Faction>()
	lateinit var grid: Array2D<ZoneTile>
	val levelRange: Range = Range(Point((zoneIndex - 1) * zoneLevelRange + 1, zoneIndex * zoneLevelRange))

	val symbols = ObjectMap<Char, ZoneSymbol>()

	lateinit var theme: Theme
	val possibleLevels = Array<XmlData>()

	lateinit var encounters: kotlin.Array<Encounter>

	val width: Int
		get() = grid.width

	val height: Int
		get() = grid.height

	fun createGrid(xmlData: XmlData)
	{
		val charGrid = xmlData.toCharGrid()

		grid = Array2D(charGrid.width, charGrid.height) { x, y -> ZoneTile(x, y) }

		var foundEncounters = 0
		for (tile in grid)
		{
			val char = charGrid[tile.x, grid.height-tile.y-1]

			if (char == '!')
			{
				tile.sprite = symbols[','].spriteWrapper.copy()
				tile.isEncounter = true
				foundEncounters++
			}
			else if (char == 'S')
			{
				tile.sprite = symbols[','].spriteWrapper.copy()
				tile.isEncounter = true
				tile.isStart = true
				foundEncounters++
			}
			else if (char == 'B')
			{
				tile.sprite = symbols[','].spriteWrapper.copy()
				tile.isEncounter = true
				tile.isBoss = true
				foundEncounters++
			}
			else
			{
				val symbol = symbols[char] ?: throw Exception("No symbol defined for character '$char'!")

				tile.sprite = symbol.spriteWrapper.copy()

				if (symbol.isEncounter)
				{
					tile.isEncounter = true
					foundEncounters++
				}
			}

			tile.sprite.chooseSprites()
		}

		if (foundEncounters != numEncounters)
		{
			throw Exception("Invalid number of encounters defined in zone! Found $foundEncounters, should have found $numEncounters")
		}
	}

	fun createPath()
	{
		// starting at the start tile, walk to neighbouring tile, building the path
		val explored = IntSet()

		var current = grid.first{ it.isStart }
		var progression = 0
		val encountersArray = Array<Encounter>()
		while (true)
		{
			explored.add(current.hashCode())

			val alpha = progression.toFloat() / numEncounters.toFloat()
			val level = levelRange.min.lerp(levelRange.max, alpha)

			val encounter = createEncounter(level, progression, current.isBoss)
			encountersArray.add(encounter)
			current.encounter = encounter

			if (progression == numEncounters-1)
			{
				break
			}

			var found = false
			for (dir in Direction.ValuesWithoutCenter)
			{
				val nTile = grid.tryGet(current.x + dir.x, current.y + dir.y, null) ?: continue
				if (!explored.contains(nTile.hashCode()) && nTile.isEncounter)
				{
					current = nTile
					found = true
					break
				}
			}

			if (!found)
			{
				throw Exception("Unable to find next encounter! Progression: $progression, Position: $current")
			}

			progression++
		}

		encounters = kotlin.Array(encountersArray.size) { i -> encountersArray[i] }

		updateFlags()
	}

	fun createEncounter(level: Float, progression: Int, isBoss: Boolean): Encounter
	{
		// create hero pool
		val entities = Array<FactionEntity>()
		for (faction in factions)
		{
			for (entity in faction.heroes)
			{
				if (entity.rarity.ordinal >= Rarity.RARE.ordinal && progression < numEncounters*0.4) continue
				if (entity.rarity.ordinal >= Rarity.SUPERRARE.ordinal && progression < numEncounters*0.5) continue
				if (entity.rarity.ordinal >= Rarity.ULTRARARE.ordinal && progression < numEncounters*0.7) continue

				var spawnWeight = entity.rarity.spawnWeight
				if (isBoss && entity.rarity.ordinal >= Rarity.RARE.ordinal)
				{
					spawnWeight *= 3
				}

				for (i in 0 until spawnWeight)
				{
					entities.add(entity)
				}
			}
		}

		val encounter = Encounter(this, level.toInt(), progression, isBoss, ran.nextLong())
		encounter.gridEl = possibleLevels.random(ran)

		// generate hero levels
		val levels = kotlin.Array<Int>(5) { level.toInt() }
		val levelRaisedPool = (0..4).toGdxArray()
		var remainder = level - levels[0]
		while (remainder >= 0.2f)
		{
			remainder -= 0.2f
			levels[levelRaisedPool.removeRandom(ran)]++
		}

		// generate hero ascensions
		val ascension = level / levelsPerAscension.toFloat()
		val ascensions = kotlin.Array<Int>(5) { ascension.toInt() }
		val ascensionRaisedPool = (0..4).toGdxArray()
		remainder = ascension - ascensions[0]
		while (remainder >= 0.2f)
		{
			remainder -= 0.2f
			ascensions[ascensionRaisedPool.removeRandom(ran)]++
		}

		// generate the heroes
		for (i in 0 until 5)
		{
			val level = levels[i]
			val ascension = Ascension.get(ascensions[i])
			val heroData = entities.removeRandom(ran)
			val entityData = EntityData(heroData, ascension, level)

			encounter.enemies.add(entityData)
		}

		// generate hero equipment
		class HeroEquipmentSlot(val hero: EntityData, val slot: EquipmentSlot, val weight: EquipmentWeight)
		val heroEquipmentSlots = Array<HeroEquipmentSlot>()
		for (hero in encounter.enemies)
		{
			val weight = hero.getEntity("1").stats().equipmentWeight
			for (slot in EquipmentSlot.Values)
			{
				heroEquipmentSlots.add(HeroEquipmentSlot(hero, slot, weight))
			}
		}
		val equipmentAlpha = level / levelsPerEquipmentAscension.toFloat()
		val baseEquipmentAlpha = equipmentAlpha.toInt()
		val remainderEquipmentAlpha = equipmentAlpha - baseEquipmentAlpha
		// fill improved
		val numImproved = (remainderEquipmentAlpha / (1f / heroEquipmentSlots.size)).toInt()
		for (i in 0 until numImproved)
		{
			val heroSlot = heroEquipmentSlots.removeRandom(ran)
			val ascension = Ascension.Values[baseEquipmentAlpha]
			val equip = EquipmentCreator.createRandom(level.toInt(), ran, heroSlot.slot, heroSlot.weight, ascension)
			heroSlot.hero.equipment[heroSlot.slot] = equip
		}

		// fill base
		if (baseEquipmentAlpha > 0)
		{
			for (heroSlot in heroEquipmentSlots)
			{
				val ascension = Ascension.Values[baseEquipmentAlpha-1]
				val equip = EquipmentCreator.createRandom(level.toInt(), ran, heroSlot.slot, heroSlot.weight, ascension)
				heroSlot.hero.equipment[heroSlot.slot] = equip
			}
		}

		return encounter
	}

	fun updateFlags()
	{
		for (tile in grid)
		{
			tile.updateFlag()
		}
	}

	companion object
	{
		val numEncounters = 80
		val zoneLevelRange = 15
		val levelsPerAscension = 30
		val levelsPerEquipmentAscension = 40

		fun load(index: Int): Zone
		{
			var testI = index
			var xml: XmlData
			while (true)
			{
				try
				{
					val path = "Zones/Zone$testI"
					xml = getXml(path)
					break
				}
				catch (ex: Exception)
				{
					testI--
				}
			}

			val zone = Zone(index)

			val factionsEl = xml.getChildByName("Factions")!!
			for (el in factionsEl.children)
			{
				zone.factions.add(Faction.load(el.text))
			}

			val gridEl = xml.getChildByName("Grid")!!

			val symbolsEl = xml.getChildByName("Symbols")!!
			for (symbolEl in symbolsEl.children)
			{
				val char = symbolEl.get("Character")[0]
				val sprite = SpriteWrapper.load(symbolEl.getChildByName("Sprite")!!)
				val isEncounter = symbolEl.getBoolean("IsEncounter", false)

				zone.symbols[char] = ZoneSymbol(char, sprite, isEncounter)
			}

			zone.theme = Theme.load(xml.get("Theme"))

			val levelsEl = xml.getChildByName("Levels")!!
			for (el in levelsEl.children)
			{
				zone.possibleLevels.add(el)
			}

			zone.createGrid(gridEl)
			zone.createPath()

			return zone
		}
	}
}

class ZoneSymbol(val char: Char, val spriteWrapper: SpriteWrapper, val isEncounter: Boolean)

class ZoneTile(x: Int, y: Int) : Point(x, y), IPathfindingTile
{
	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		return true
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?): Int
	{
		return 0
	}

	var isEncounter = false
	var isBoss = false
	var isStart = false

	var encounter: Encounter? = null

	var flagSprite: Sprite? = null

	lateinit var sprite: SpriteWrapper

	fun updateFlag()
	{
		if (isEncounter)
		{
			if (encounter!!.progression == Global.data.currentZoneProgression)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_combat", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else if (encounter!!.progression < Global.data.currentZoneProgression)
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_complete", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}
			else
			{
				flagSprite = AssetManager.loadSprite("Oryx/Custom/terrain/flag_enemy", drawActualSize = true)
				flagSprite!!.randomiseAnimation()
			}

			if (isBoss)
			{
				flagSprite!!.baseScale[0] = 1.5f
				flagSprite!!.baseScale[1] = 1.5f
				flagSprite!!.colour = Colour(1f, 0.9f, 0.9f, 1f)
			}
			else
			{
				//flagSprite!!.baseScale[0] = 0.8f
				//flagSprite!!.baseScale[1] = 0.8f
			}
		}
	}
}

class Encounter(val zone: Zone, val level: Int, val progression: Int, val isBoss: Boolean, val seed: Long)
{
	val enemies = Array<EntityData>(5)
	lateinit var gridEl: XmlData

	fun createLevel(theme: Theme): Level
	{
		val level = Level.load(gridEl, theme)

		val ran = Random.obtainTS(seed)
		val bossIndex = if (isBoss) ran.nextInt(5) else -1

		var i = 0
		for (enemy in enemies)
		{
			val entity = enemy.getEntity("2")
			level.enemyTiles[i].entity = entity
			entity.pos().tile = level.enemyTiles[i].tile
			entity.pos().addToTile(entity)

			val handicapLevelDiff = entity.stats().level - 5
			if (handicapLevelDiff < 0)
			{
				entity.stats().statModifier = handicapLevelDiff * 0.05f
			}

			if (i == bossIndex)
			{
				entity.stats().statModifier += 0.4f
				entity.stats().ascension = entity.stats().ascension.nextAscension
				entity.directionalSprite().directionalSprite.scale = 1.2f

				val bossBuff = Buff()
				bossBuff.statistics[Statistic.BUFFPOWER] = 0.4f
				bossBuff.statistics[Statistic.DEBUFFPOWER] = 0.4f
				entity.stats().buffs.add(bossBuff)

				var additionalRenderableComponent = entity.additionalRenderable()
				if (additionalRenderableComponent == null)
				{
					additionalRenderableComponent = AdditionalRenderableComponent()
					entity.add(additionalRenderableComponent)
				}

				additionalRenderableComponent.below["BossAura"] = AssetManager.loadParticleEffect("BossAura").getParticleEffect()
			}

			entity.stats().resetHP()

			i++
		}

		ran.freeTS()

		return level
	}

	class RewardDesc(val tile: Table, val func: (src: Vector2, stage: Stage, gameDataBar: GameDataBar, navigationBar: NavigationBar) -> Unit)
	fun generateRewards(victory: Boolean): Array<RewardDesc>
	{
		val ran = Random.obtainTS(seed+100)

		val output = Array<RewardDesc>()

		// experience

		val expRequired = Global.getExperienceForLevel(level)
		var expReward = (expRequired / 5f).toInt()
		expReward += (expReward * ((ran.nextFloat() * 0.4f) - 0.2f)).toInt() // +/-20%

		if (isBoss)
		{
			expReward += (expRequired / 5f).toInt()
		}

		if (!victory)
		{
			expReward = (expReward * 0.4f).toInt()
		}

		val expTile = Stack()
		expTile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), 64f, 64f))
		expTile.add(SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/book_blue"), 64f, 64f))

		val expAmountTable = Table()
		expAmountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		expAmountTable.add(Label(expReward.prettyPrint(), Global.skin, "small")).padBottom(5f)

		expTile.addTable(expAmountTable).expandY().growX().bottom()
		expTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 64f, 64f))

		val expTable = Table()
		expTable.add(expTile).grow()
		expTable.addTapToolTip("Gain ${expReward.prettyPrint()} experience.")

		output.add(RewardDesc(expTable, { src, stage, gameDataBar, navigationBar ->

			Future.call({ Global.data.experience += expReward }, 1f)

			var delay = 0f
			for (i in 0 until 15)
			{
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
					Actions.alpha(0f) then
						Actions.delay(delay) then
						Actions.alpha(1f) then
						Actions.parallel(
							mote(src, dst, 1f, Interpolation.exp5, false),
							Actions.scaleTo(0.7f, 0.7f, 1f),
							Actions.delay(0.8f) then lambda { stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
						Actions.removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)

				delay += 0.01f
			}
		}))

		// gold
		var goldReward = 400 + level * 2
		if (isBoss)
		{
			goldReward += 400
		}

		goldReward += (goldReward * ((ran.nextFloat() * 0.4f) - 0.2f)).toInt() // +/-20%

		if (!victory)
		{
			goldReward = (goldReward * 0.4f).toInt()
		}

		val goldTile = Stack()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/textured_back"), 64f, 64f))
		goldTile.add(SpriteWidget(AssetManager.loadSprite("Oryx/Custom/items/coin_gold_pile"), 64f, 64f))

		val goldAmountTable = Table()
		goldAmountTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.6f))
		goldAmountTable.add(Label(goldReward.prettyPrint(), Global.skin, "small")).padBottom(5f)

		goldTile.addTable(goldAmountTable).expandY().growX().bottom()
		goldTile.add(SpriteWidget(AssetManager.loadSprite("GUI/PortraitFrameBorder"), 64f, 64f))

		val goldTable = Table()
		goldTable.add(goldTile).grow()
		expTable.addTapToolTip("Gain ${goldReward.prettyPrint()} gold.")

		output.add(RewardDesc(goldTable, { src, stage, gameDataBar, navigationBar ->
			Future.call({ Global.data.gold += goldReward }, 1f)

			var delay = 0f
			for (i in 0 until 15)
			{
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
					Actions.alpha(0f) then
						Actions.delay(delay) then
						Actions.alpha(1f) then
						Actions.parallel(
							mote(src, dst, 1f, Interpolation.exp5, false),
							Actions.scaleTo(0.7f, 0.7f, 1f),
							Actions.delay(0.8f) then lambda { stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
						Actions.removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)

				delay += 0.01f
			}
		}))

		// chance for equipment
		if (victory && (isBoss || ran.nextFloat() < 0.2f))
		{
			val equip = EquipmentCreator.createRandom(level)
			val tile = equip.createTile(64f)
			tile.addTapToolTip("Gain the equipment ${equip.fullName}.")

			output.add(RewardDesc(tile, { src, stage, gameDataBar, navigationBar ->
				Global.data.equipment.add(equip)

				val dstTable = navigationBar.screenMap[MainGame.ScreenEnum.HEROES]
				val dst = dstTable.localToStageCoordinates(Vector2())

				val widget = MaskedTexture(equip.fullIcon)
				widget.setSize(48f, 48f)
				widget.setPosition(src.x, src.y)
				widget.toFront()

				val sparkleParticle = ParticleEffectActor(AssetManager.loadParticleEffect("GetEquipment", timeMultiplier = 1.2f).getParticleEffect(), true)
				sparkleParticle.color = Color.WHITE.cpy().lerp(equip.ascension.colour.color(), 0.3f)
				sparkleParticle.setSize(dstTable.height, dstTable.height)
				sparkleParticle.setPosition(dstTable.x + dstTable.width*0.5f - dstTable.height * 0.5f, dstTable.y)

				val sequence =
					Actions.parallel(
						mote(src, dst, 1f, Interpolation.exp5, false),
						Actions.scaleTo(0.7f, 0.7f, 1f),
						Actions.delay(0.9f) then lambda { stage.addActor(sparkleParticle) } then Actions.fadeOut(0.1f)) then
						Actions.removeActor()
				widget.addAction(sequence)

				stage.addActor(widget)
			}))
		}

		ran.freeTS()

		return output
	}
}