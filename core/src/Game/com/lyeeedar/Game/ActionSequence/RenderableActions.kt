package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.*
import ktx.collections.set

enum class OnEndBehaviour
{
	NOTHING,
	KILL,
	STOP
}

class ReplaceSourceRenderableAction() : AbstractActionSequenceAction()
{
	lateinit var renderable: Renderable

	var blendDuration = 0f

	var restoreOriginal = true
	var originalRenderable: Renderable? = null

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		val source = sequence.source

		if (source.renderableOrNull() == null)
		{
			source.add(RenderableComponent.obtain())
		}
		else
		{
			originalRenderable = source.renderable().renderable
		}

		val newRenderable = renderable.copy()
		source.renderable().renderable = newRenderable
		source.renderable().lockRenderable = true

		if (originalRenderable != null && blendDuration > 0f)
		{
			var addRenderable = source.additionalRenderable()
			if (addRenderable == null)
			{
				addRenderable = AdditionalRenderableComponent.obtain()
				source.add(addRenderable)
			}

			addRenderable.above["blendTo"] = originalRenderable!!
			originalRenderable!!.animation = AlphaAnimation.obtain().set(blendDuration, 1f, 0f)
			originalRenderable!!.animation = ColourChangeAnimation.obtain().set(Colour.WHITE, Colour.WHITE.copy().mul(50f), blendDuration)
			Future.call({ addRenderable.above.remove("blendTo") }, blendDuration*0.98f)

			newRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 0f, 1f)
			newRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE.copy().mul(50f), Colour.WHITE, blendDuration)
		}

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		if (!restoreOriginal) return

		val source = sequence.source

		if (originalRenderable != null)
		{
			val replacementRenderable = source.renderable().renderable

			source.renderable().renderable = originalRenderable!!
			source.renderable().lockRenderable = false

			if (blendDuration > 0f)
			{
				var addRenderable = source.additionalRenderable()
				if (addRenderable == null)
				{
					addRenderable = AdditionalRenderableComponent.obtain()
					source.add(addRenderable)
				}

				addRenderable.above["blendFrom"] = replacementRenderable
				replacementRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 1f, 0f)
				replacementRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE, Colour.WHITE.copy().mul(50f), blendDuration)
				Future.call({ addRenderable.above.remove("blendFrom") }, blendDuration*0.95f)

				originalRenderable!!.animation = AlphaAnimation.obtain().set(blendDuration, 0f, 1f)
				originalRenderable!!.animation = ColourChangeAnimation.obtain().set(Colour.WHITE.copy().mul(50f), Colour.WHITE, blendDuration)
			}

		}
		else
		{
			source.remove(RenderableComponent::class.java)
		}

		originalRenderable = null
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = ReplaceSourceRenderableAction.obtain()

		action.renderable = renderable
		action.restoreOriginal = restoreOriginal
		action.blendDuration = blendDuration

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		renderable = AssetManager.loadRenderable(xmlData.getChildByName("Renderable")!!)
		restoreOriginal = xmlData.getBoolean("RestoreOriginal", true)
		blendDuration = xmlData.getFloat("BlendDuration", 0f)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ReplaceSourceRenderableAction> = object : Pool<ReplaceSourceRenderableAction>() {
			override fun newObject(): ReplaceSourceRenderableAction
			{
				return ReplaceSourceRenderableAction()
			}

		}

		@JvmStatic fun obtain(): ReplaceSourceRenderableAction
		{
			val obj = ReplaceSourceRenderableAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { ReplaceSourceRenderableAction.pool.free(this); obtained = false } }
}

class SourceAnimationAction() : AbstractActionSequenceAction()
{
	enum class Animation
	{
		EXPAND,
		SPIN,
		FADE
	}

	lateinit var anim: Animation

	var startSize = 1f
	var endSize = 1f
	var oneWay = true

	var startFade = 1f
	var endFade = 1f

	var spinAngle = 0f

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		val source = sequence.source
		val sourceRenderable = source.renderableOrNull()?.renderable ?: return false

		val duration = end - start
		if (anim == Animation.EXPAND)
		{
			sourceRenderable.animation = ExpandAnimation.obtain().set(duration, startSize, endSize, oneWay)
		}
		else if (anim == Animation.SPIN)
		{
			sourceRenderable.animation = SpinAnimation.obtain().set(duration, spinAngle)
		}
		else if (anim == Animation.FADE)
		{
			sourceRenderable.animation = AlphaAnimation.obtain().set(duration, startFade, endFade)
		}
		else
		{
			throw Exception("Unhandled animation type '$anim'!")
		}

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = SourceAnimationAction.obtain()

		action.anim = anim
		action.startSize = startSize
		action.endSize = endSize
		action.oneWay = oneWay
		action.spinAngle = spinAngle
		action.startFade = startFade
		action.endFade = endFade

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		anim = Animation.valueOf(xmlData.get("Animation", "Expand")!!.toUpperCase())
		startSize = xmlData.getFloat("SizeStart", 1f)
		endSize = xmlData.getFloat("SizeEnd", 1f)
		oneWay = xmlData.getBoolean("OneWay", false)
		spinAngle = xmlData.getFloat("Angle", 0f)
		startFade = xmlData.getFloat("FadeStart", 1f)
		endFade = xmlData.getFloat("FadeEnd", 1f)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SourceAnimationAction> = object : Pool<SourceAnimationAction>() {
			override fun newObject(): SourceAnimationAction
			{
				return SourceAnimationAction()
			}

		}

		@JvmStatic fun obtain(): SourceAnimationAction
		{
			val obj = SourceAnimationAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SourceAnimationAction.pool.free(this); obtained = false } }
}

class DestinationRenderableAction() : AbstractActionSequenceAction()
{
	enum class SpawnBehaviour
	{
		IMMEDIATE,
		FROMSOURCE,
		FROMCENTER,
		RANDOM
	}

	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	var entityPerTile = false
	lateinit var onEnd: OnEndBehaviour
	var alignToVector = true

	var spawnBehaviour: SpawnBehaviour = SpawnBehaviour.IMMEDIATE
	var spawnDuration = 0f

	val entities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		if (Global.resolveInstant || sequence.targets.size == 0) return false

		val min = sequence.targets.minBy(Point::hashCode)!!
		val max = sequence.targets.maxBy(Point::hashCode)!!

		if (entityPerTile)
		{
			val sourceTile = sequence.source.tile()!!

			val furthest = sequence.targets.maxBy { it.taxiDist(sourceTile) }!!

			for (point in sequence.targets)
			{
				val tile = sequence.level.getTile(point) ?: continue

				val entity = EntityPool.obtain()

				val r = renderable.getParticleEffect()
				if (spawnBehaviour == SpawnBehaviour.IMMEDIATE)
				{
					// do nothing
				}
				else if (spawnBehaviour == SpawnBehaviour.FROMSOURCE)
				{
					val maxDist = furthest.euclideanDist(sourceTile)
					val dist = tile.euclideanDist(sourceTile)
					val alpha = dist / maxDist
					val delay = spawnDuration * alpha

					r.renderDelay = delay
				}
				else if (spawnBehaviour == SpawnBehaviour.FROMCENTER)
				{
					val center = min.lerp(max, 0.5f)
					val maxDist = center.euclideanDist(max)
					val dist = center.euclideanDist(tile)
					val alpha = dist / maxDist
					val delay = spawnDuration * alpha

					r.renderDelay = delay
				}
				else if (spawnBehaviour == SpawnBehaviour.RANDOM)
				{
					val alpha = Random.random()
					val delay = spawnDuration * alpha

					r.renderDelay = delay
				}
				else
				{
					throw Exception("Unhandled spawn behaviour")
				}

				entity.add(RenderableComponent.obtain().set(r))
				entity.add(PositionComponent.obtain())
				val pos = entity.pos()

				pos.position = tile
				pos.slot = slot

				if (alignToVector)
				{
					pos.facing = sequence.facing
				}

				Global.engine.addEntity(entity)

				entities.add(entity)
			}
		}
		else
		{
			if (sequence.targets.size == 0) return false

			val entity = EntityPool.obtain()

			val r = renderable.getParticleEffect()
			entity.add(RenderableComponent.obtain().set(r))
			entity.add(PositionComponent.obtain())
			val pos = entity.pos()

			pos.min = min
			pos.max = max
			pos.slot = slot

			if (alignToVector)
			{
				pos.facing = sequence.facing
			}

			r.size[0] = (pos.max.x - pos.min.x) + 1
			r.size[1] = (pos.max.y - pos.min.y) + 1

			if (alignToVector && r.useFacing)
			{
				r.rotation = pos.facing.angle
				r.facing = pos.facing

				if (pos.facing.x != 0)
				{
					val temp = r.size[0]
					r.size[0] = r.size[1]
					r.size[1] = temp
				}
			}

			Global.engine.addEntity(entity)

			entities.add(entity)
		}

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		for (entity in entities)
		{
			val renderable = entity.renderable().renderable
			if (renderable is ParticleEffect)
			{
				if (onEnd == OnEndBehaviour.KILL)
				{
					Global.engine.removeEntity(entity)
				}
				else if (onEnd == OnEndBehaviour.STOP)
				{
					renderable.stop()
				}
			}
			else
			{
				Global.engine.removeEntity(entity)
			}
		}
		entities.clear()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val out = DestinationRenderableAction.obtain()
		out.renderable = renderable
		out.slot = slot
		out.onEnd = onEnd
		out.alignToVector = alignToVector
		out.entityPerTile = entityPerTile
		out.spawnBehaviour = spawnBehaviour
		out.spawnDuration = spawnDuration

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		entityPerTile = xmlData.getBoolean("RenderablePerTile", false)
		onEnd = OnEndBehaviour.valueOf(xmlData.get("OnEnd", "Stop")!!.toUpperCase())
		alignToVector = xmlData.getBoolean("AlignToVector", true)
		spawnBehaviour = SpawnBehaviour.valueOf(xmlData.get("SpawnBehaviour", "Immediate")!!.toUpperCase())
		spawnDuration = xmlData.getFloat("SpawnDuration", 0f)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DestinationRenderableAction> = object : Pool<DestinationRenderableAction>() {
			override fun newObject(): DestinationRenderableAction
			{
				return DestinationRenderableAction()
			}

		}

		@JvmStatic fun obtain(): DestinationRenderableAction
		{
			val obj = DestinationRenderableAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { DestinationRenderableAction.pool.free(this); obtained = false } }
}

class AttachToEntityRenderableAction() : AbstractActionSequenceAction()
{
	enum class SpawnBehaviour
	{
		IMMEDIATE,
		FROMSOURCE,
		FROMCENTER,
		RANDOM
	}

	enum class SelectionMode
	{
		ANY,
		ALLIES,
		ENEMIES
	}

	lateinit var renderable: ParticleEffectDescription
	var above = true

	var spawnBehaviour: SpawnBehaviour = SpawnBehaviour.IMMEDIATE
	var spawnDuration = 0f
	var selectionMode: SelectionMode = SelectionMode.ANY

	val attachGuid = this.hashCode().toString()
	val entities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		if (Global.resolveInstant || sequence.targets.size == 0) return false

		val min = sequence.targets.minBy(Point::hashCode)!!
		val max = sequence.targets.maxBy(Point::hashCode)!!

		val furthest = sequence.targets.maxBy { it.taxiDist(sequence.source.tile()!!) }!!

		for (point in sequence.targets)
		{
			val tile = sequence.level.getTile(point) ?: continue
			val entity = tile.firstEntity() ?: continue

			if (selectionMode == SelectionMode.ALLIES && !sequence.source.isAllies(entity)) continue
			if (selectionMode == SelectionMode.ENEMIES && !sequence.source.isEnemies(entity)) continue

			val r = renderable.getParticleEffect()
			if (spawnBehaviour == SpawnBehaviour.IMMEDIATE)
			{
				// do nothing
			}
			else if (spawnBehaviour == SpawnBehaviour.FROMSOURCE)
			{
				val maxDist = furthest.euclideanDist(sequence.source.tile()!!)
				val dist = tile.euclideanDist(sequence.source.tile()!!)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				r.renderDelay = delay
			}
			else if (spawnBehaviour == SpawnBehaviour.FROMCENTER)
			{
				val center = min.lerp(max, 0.5f)
				val maxDist = center.euclideanDist(max)
				val dist = center.euclideanDist(tile)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				r.renderDelay = delay
			}
			else if (spawnBehaviour == SpawnBehaviour.RANDOM)
			{
				val alpha = Random.random()
				val delay = spawnDuration * alpha

				r.renderDelay = delay
			}
			else
			{
				throw Exception("Unhandled spawn behaviour")
			}

			var additionalRenderableComponent = entity.additionalRenderable()
			if (additionalRenderableComponent == null)
			{
				additionalRenderableComponent = AdditionalRenderableComponent.obtain()
				entity.add(additionalRenderableComponent)
			}

			if (above)
			{
				additionalRenderableComponent.above[attachGuid] = r
			}
			else
			{
				additionalRenderableComponent.below[attachGuid] = r
			}

			entities.add(entity)
		}

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		for (entity in entities)
		{
			val additionalRenderableComponent = entity.additionalRenderable() ?: continue
			if (above)
			{
				val r = additionalRenderableComponent.above[attachGuid] as ParticleEffect
				additionalRenderableComponent.above.remove(attachGuid)

				r.stop()
				r.addToEngine(entity.pos().position)
			}
			else
			{
				val r = additionalRenderableComponent.below[attachGuid] as ParticleEffect
				additionalRenderableComponent.below.remove(attachGuid)

				r.stop()
				r.addToEngine(entity.pos().position)
			}
		}
		entities.clear()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val out = AttachToEntityRenderableAction.obtain()
		out.renderable = renderable
		out.above = above
		out.spawnBehaviour = spawnBehaviour
		out.spawnDuration = spawnDuration
		out.selectionMode = selectionMode

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		above = xmlData.getBoolean("Above", true)
		spawnBehaviour = SpawnBehaviour.valueOf(xmlData.get("SpawnBehaviour", "Immediate")!!.toUpperCase())
		spawnDuration = xmlData.getFloat("SpawnDuration", 0f)
		selectionMode = SelectionMode.valueOf(xmlData.get("SelectionMode", "Any")!!.toUpperCase())
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<AttachToEntityRenderableAction> = object : Pool<AttachToEntityRenderableAction>() {
			override fun newObject(): AttachToEntityRenderableAction
			{
				return AttachToEntityRenderableAction()
			}

		}

		@JvmStatic fun obtain(): AttachToEntityRenderableAction
		{
			val obj = AttachToEntityRenderableAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { AttachToEntityRenderableAction.pool.free(this); obtained = false } }
}

class SourceRenderableAction() : AbstractActionSequenceAction()
{
	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	lateinit var onEnd: OnEndBehaviour

	val entities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		val tile = sequence.source.tile()!!
		val entity = EntityPool.obtain()

		val r = renderable.getParticleEffect()
		entity.add(RenderableComponent.obtain().set(r))
		entity.add(PositionComponent.obtain())
		val pos = entity.pos()

		pos.position = tile
		pos.slot = slot

		Global.engine.addEntity(entity)

		entities.add(entity)

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		for (entity in entities)
		{
			val renderable = entity.renderable().renderable
			if (renderable is ParticleEffect)
			{
				if (onEnd == OnEndBehaviour.KILL)
				{
					Global.engine.removeEntity(entity)
				}
				else if (onEnd == OnEndBehaviour.STOP)
				{
					renderable.stop()
				}
			}
			else
			{
				Global.engine.removeEntity(entity)
			}
		}
		entities.clear()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val out = SourceRenderableAction.obtain()
		out.renderable = renderable
		out.slot = slot
		out.onEnd = onEnd

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		onEnd = OnEndBehaviour.valueOf(xmlData.get("OnEnd", "Stop")!!.toUpperCase())
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<SourceRenderableAction> = object : Pool<SourceRenderableAction>() {
			override fun newObject(): SourceRenderableAction
			{
				return SourceRenderableAction()
			}

		}

		@JvmStatic fun obtain(): SourceRenderableAction
		{
			val obj = SourceRenderableAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { SourceRenderableAction.pool.free(this); obtained = false } }
}

class MovementRenderableAction() : AbstractActionSequenceAction()
{
	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	var useLeap: Boolean = false
	var origin: String? = null

	lateinit var entity: Entity

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		entity = EntityPool.obtain()

		if (sequence.targets.size == 0) return false

		val r = renderable.getParticleEffect()
		entity.add(RenderableComponent.obtain().set(r))
		entity.add(PositionComponent.obtain())
		val pos = entity.pos()

		val min = sequence.targets.minBy(Point::hashCode)!!
		val max = sequence.targets.maxBy(Point::hashCode)!!
		val midPoint = min + (max - min) / 2

		pos.position = sequence.level.getTileClamped(midPoint)
		pos.slot = slot

		val originPoint: Point
		if (origin == null)
		{
			originPoint = sequence.source.tile()!!
		}
		else
		{
			val targets = sequence.storedTargets[origin!!]
						  ?: throw Exception("No stored target with name $origin! Stored targets: " + sequence.storedTargets.keys().joinToString(", "))

			originPoint = targets.firstOrNull() ?: sequence.source.tile()!!
		}

		r.rotation = getRotation(originPoint, pos.position)

		val duration = end - start
		if (useLeap)
		{
			r.animation = LeapAnimation.obtain().set(duration, pos.position.getPosDiff(originPoint), 2f)
			r.animation = ExpandAnimation.obtain().set(duration, 0.5f, 1.5f, false)
		}
		else
		{
			r.animation = MoveAnimation.obtain().set(duration, UnsmoothedPath(midPoint.getPosDiff(originPoint)), Interpolation.linear)
		}

		Global.engine.addEntity(entity)

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		Global.engine.removeEntity(entity)
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val out = MovementRenderableAction.obtain()
		out.renderable = renderable
		out.useLeap = useLeap
		out.slot = slot
		out.origin = origin

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		useLeap = xmlData.getBoolean("UseLeap", false)
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		origin = xmlData.get("Origin", null)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<MovementRenderableAction> = object : Pool<MovementRenderableAction>() {
			override fun newObject(): MovementRenderableAction
			{
				return MovementRenderableAction()
			}

		}

		@JvmStatic fun obtain(): MovementRenderableAction
		{
			val obj = MovementRenderableAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { MovementRenderableAction.pool.free(this); obtained = false } }
}

class ScreenShakeAction() : AbstractActionSequenceAction()
{
	var speed: Float = 0f
	var amount: Float = 0f

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		Global.engine.render()?.renderer?.setScreenShake(amount, speed)
		Global.engine.render()?.renderer?.lockScreenShake()

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		Global.engine.render()?.renderer?.unlockScreenShake()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val out = ScreenShakeAction.obtain()
		out.speed = speed
		out.amount = amount

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		this.speed = 1f / xmlData.getFloat("Speed", 10f)
		this.amount = xmlData.getFloat("Strength", 5f)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ScreenShakeAction> = object : Pool<ScreenShakeAction>() {
			override fun newObject(): ScreenShakeAction
			{
				return ScreenShakeAction()
			}

		}

		@JvmStatic fun obtain(): ScreenShakeAction
		{
			val obj = ScreenShakeAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { ScreenShakeAction.pool.free(this); obtained = false } }
}