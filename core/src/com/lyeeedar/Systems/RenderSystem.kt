package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Statistic
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import ktx.math.plus
import ktx.math.times

class RenderSystem(): AbstractSystem(Family.all(PositionComponent::class.java).one(RenderableComponent::class.java).get())
{
	val numHpPips = 10

	val shape: ShapeRenderer by lazy { ShapeRenderer() }
	var drawParticleDebug = false
	var drawEmitters = false
	var drawParticles = false
	var drawTargetting = false
	val particles = Array<ParticleEffect>()

	val tileCol = Colour()
	val hpBarCol = Colour()

	var tileSize = 40f
		set(value)
		{
			if (field != value)
			{
				field = value
				renderer.tileSize = value
				needsStaticRender = true
			}
		}

	val white = AssetManager.loadTextureRegion("Sprites/white.png")!!
	val hp_border = AssetManager.loadTextureRegion("Sprites/GUI/health_border.png")!!

	val border = AssetManager.loadTextureRegion("Sprites/GUI/border")!!
	val team1Col = Colour.CYAN.copy().a(0.7f)
	val team2Col = Colour.RED.copy().a(0.7f)

	val team1HpCol = Colour.CYAN.copy()
	val team2HpCol = Colour.RED.copy()
	val healCol = Colour.GREEN.copy()
	val lostHpCol = Colour.ORANGE.copy()
	val emptyCol = Colour.BLACK.copy()

	lateinit var renderer: SortedRenderer
	val screenSpaceRenderer = SortedRenderer(Global.resolution[0].toFloat(), 1f, 1f, 1, true)

	override fun registerDebugCommands(debugConsole: DebugConsole)
	{
		debugConsole.register("ParticleDebug", "'ParticleDebug (Emitter|Particle) true' to enable, 'ParticleDebug (Emitter|Particle) false' to disable", fun (args, console): Boolean {
			if (args[0] == "false" && args.size == 1)
			{
				console.write("Particle debug draw disabled")

				drawEmitters = false
				drawParticles = false
				drawParticleDebug = false
				return true
			}
			else if (args[0] == "true" && args.size == 1)
			{
				console.write("Particle debug draw enabled")

				drawEmitters = true
				drawParticles = true
				drawParticleDebug = true
				return true
			}
			else
			{
				val changingEmitter = args.contains("emitter")
				val changingParticle = args.contains("particle")

				val isTrue = args.contains("true")
				val isFalse = args.contains("false")

				if (isTrue == isFalse || !(changingEmitter || changingParticle))
				{
					return false
				}

				if (changingEmitter) drawEmitters = isTrue
				if (changingParticle) drawParticles = isTrue

				console.write("Enable particle debug")
				return true
			}
		})
	}

	override fun unregisterDebugCommands(debugConsole: DebugConsole)
	{
		debugConsole.unregister("ParticleDebug")
	}

	var needsStaticRender = true
	override fun onLevelChanged()
	{
		if (level != null)
		{
			renderer = SortedRenderer(tileSize, level!!.width.toFloat(), level!!.height.toFloat(), SpaceSlot.Values.size, true)
		}
		needsStaticRender = true
	}

	var deltaTime: Float = 0f
	override fun doUpdate(deltaTime: Float)
	{
		this.deltaTime = deltaTime
	}

	fun doRender(batch: Batch, renderArea: Widget)
	{
		val level = level ?: return

		batch.end()

		val w = renderArea.width / level.width.toFloat()
		val h = (renderArea.height - 16f) / level.height.toFloat()

		tileSize = Math.min(w, h)
		renderer.tileSize = tileSize

		val xp = renderArea.x + (renderArea.width / 2f) - ((level.width * tileSize) / 2f)
		val yp = renderArea.y

		if (needsStaticRender)
		{
			needsStaticRender = false

			renderer.beginStatic()
			for (x in 0 until level.grid.width)
			{
				for (y in 0 until level.grid.height)
				{
					val tile = level.grid[x, y]
					val tileColour = Colour.WHITE

					val xi = x.toFloat()
					val yi = y.toFloat()

					val spriteWrapper = tile.sprite ?: continue
					if (!spriteWrapper.hasChosenSprites)
					{
						spriteWrapper.chooseSprites()
					}

					val sprite = tile.sprite?.chosenSprite
					if (sprite != null)
					{
						renderer.queueSprite(sprite, xi, yi, 0, 0, tileColour)
					}

					val tilingSprite = tile.sprite?.chosenTilingSprite
					if (tilingSprite != null)
					{
						renderer.queueSprite(tilingSprite, xi, yi, 0, 0, tileColour)
					}
				}
			}
			renderer.endStatic(batch)
		}

		renderer.begin(deltaTime, xp, yp, level.ambient)

		for (entity in entities)
		{
			val renderable = entity.renderable()?.renderable ?: continue
			val pos = entity.pos() ?: continue
			val tile = entity.tile()

			val px = pos.position.x.toFloat()
			val py = pos.position.y.toFloat()

			if (renderable is ParticleEffect)
			{
				val effect = renderable
				if (effect.completed && Mappers.transient.get(entity) != null)
				{
					entity.add(MarkedForDeletionComponent())
				}
			}

			tileCol.set(Colour.WHITE)

			if (tile != null)
			{
				if (tile.isSelectedPoint)
				{
					tileCol.mul(0.6f, 1.2f, 0.6f, 1.0f)
				}
				else
				{
					if (tile.isValidTarget) tileCol.mul(0.65f, 0.65f, 0.4f, 1.0f)
					if (tile.isValidHitPoint) tileCol.mul(1.2f, 0.7f, 0.7f, 1.0f)
				}
			}

			renderer.queue(renderable, px, py, pos.slot.ordinal, 0, tileCol)

			if (drawParticleDebug && renderable is ParticleEffect)
			{
				particles.add(renderable)
			}

			val offset = renderable.animation?.renderOffset(false)

			var ax = px
			var ay = py

			if (offset != null)
			{
				ax += offset[0]
				ay += offset[1]
			}

			val additional = entity.additionalRenderable()
			if (additional != null)
			{
				for (below in additional.below.values())
				{
					renderer.queue(below, ax, ay, pos.slot.ordinal, -1, tileCol)

					if (drawParticleDebug && below is ParticleEffect)
					{
						particles.add(below)
					}

				}

				for (above in additional.above.values())
				{
					renderer.queue(above, ax, ay, pos.slot.ordinal, 1, tileCol)

					if (drawParticleDebug && above is ParticleEffect)
					{
						particles.add(above)
					}
				}
			}

			val stats = entity.stats()
			if (stats != null)
			{
				renderer.queueTexture(border, ax+0.5f, ay+0.4f, -1, 0, if (stats.faction == "1") team1Col else team2Col, width = 0.9f, height = 0.7f, sortX = ax, sortY = ay)

				val hpColour = if (stats.faction == "1") team1Col else team2Col

				val totalWidth = pos.size.toFloat() * 0.95f

				val hp = stats.hp
				val maxhp = stats.getStat(Statistic.MAXHEALTH)

				val solidSpaceRatio = 0.05f
				val space = totalWidth
				val spacePerPip = space / numHpPips
				val spacing = spacePerPip * solidSpaceRatio
				val solid = spacePerPip - spacing

				var overhead = totalWidth
				if (renderable is Sprite && renderable.drawActualSize)
				{
					val region = renderable.currentTexture
					val height = region.regionHeight
					val ratio = height / 32f
					overhead *= ratio
				}

				renderer.queueTexture(white, ax + totalWidth*0.5f - solid*0.5f, ay+overhead, pos.slot.ordinal, 1, colour = emptyCol, width = totalWidth, height = 0.1f, sortX = ax, sortY = ay)

				val lostLen = (hp + stats.lostHp) / maxhp
				renderer.queueTexture(white, ax + totalWidth*lostLen*0.5f - solid*0.5f, ay+overhead, pos.slot.ordinal, 2, colour = lostHpCol, width = totalWidth*lostLen, height = 0.1f, sortX = ax, sortY = ay)

				val hpLen = hp / maxhp
				renderer.queueTexture(white, ax + totalWidth*hpLen*0.5f - solid*0.5f, ay+overhead, pos.slot.ordinal, 3, colour = hpColour, width = totalWidth*hpLen, height = 0.1f, sortX = ax, sortY = ay)

				for (i in 0 until numHpPips)
				{
					renderer.queueTexture(hp_border, ax+i*spacePerPip, ay+overhead, pos.slot.ordinal, 4, colour = tileCol, width = solid, height = 0.1f, sortX = ax, sortY = ay)
				}
			}
		}

		renderer.end(batch)

		screenSpaceRenderer.begin(deltaTime, 0f, 0f, level.ambient)

		val screenspaceItr = level.screenSpaceEffects.iterator()
		while (screenspaceItr.hasNext())
		{
			val effect = screenspaceItr.next()

			screenSpaceRenderer.queueParticle(effect, 0f, 0f, 0, 0)

			if (!effect.loop && effect.complete() && effect.completed)
			{
				screenspaceItr.remove()
			}
		}

		screenSpaceRenderer.end(batch)

		batch.begin()

		if (drawTargetting)
		{
			shape.projectionMatrix = Global.stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			for (entity in entities)
			{
				val ai = entity.task() ?: continue
				val target = ai.ai.root.findData<Entity>("enemy") ?: continue

				var source = entity.pos().tile!!.toVec() + Vector2(0.5f, 0.5f)
				var dest = target.pos().tile!!.toVec() + Vector2(0.5f, 0.5f)

				if (entity.renderOffset() != null)
				{
					source += Vector2(entity.renderOffset()!![0], entity.renderOffset()!![1])
				}

				if (target.renderOffset() != null)
				{
					dest += Vector2(target.renderOffset()!![0], target.renderOffset()!![1])
				}

				val offset = if (entity.stats().faction == "1") Vector2(xp, yp) else Vector2(xp + 5, yp + 5)
				shape.color = if (entity.stats().faction == "1") Color.CYAN else Color.RED
				shape.line(source * tileSize + offset, dest * tileSize + offset)
			}

			shape.end()
		}

		if (drawParticleDebug)
		{
			shape.projectionMatrix = Global.stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			for (particle in particles)
			{
				particle.debug(shape, 0f, 0f, tileSize, drawEmitters, drawParticles)
			}

			shape.end()

			particles.clear()
		}
	}
}