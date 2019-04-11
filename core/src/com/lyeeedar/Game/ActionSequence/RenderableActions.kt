package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.render
import com.lyeeedar.Util.*

class ReplaceSourceRenderableAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var renderable: Renderable

	var restoreOriginal = true
	var originalRenderable: Renderable? = null

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		val source = sequence.source

		if (source.renderable() == null)
		{
			source.add(RenderableComponent())
		}
		else
		{
			originalRenderable = source.renderable().renderable
		}

		source.renderable().renderable = renderable.copy()
		source.renderable().lockRenderable = true

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		val source = sequence.source

		if (originalRenderable != null)
		{
			source.renderable().renderable = originalRenderable!!
			source.renderable().lockRenderable = false

		}
		else
		{
			source.remove(RenderableComponent::class.java)
		}

		originalRenderable = null
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = ReplaceSourceRenderableAction(sequence)

		action.renderable = renderable
		action.restoreOriginal = restoreOriginal

		return action
	}

	override fun parse(xmlData: XmlData)
	{
		renderable = AssetManager.loadRenderable(xmlData.getChildByName("Renderable")!!)
		restoreOriginal = xmlData.getBoolean("RestoreOriginal", true)
	}
}

class SourceAnimationAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
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
		val sourceRenderable = source.renderable()?.renderable ?: return false

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

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val action = SourceAnimationAction(sequence)

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

}

class DestinationRenderableAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	var entityPerTile = false
	var killOnEnd = true
	var alignToVector = true

	val entities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		if (entityPerTile)
		{
			for (tile in sequence.targets)
			{
				val entity = Entity()

				val r = renderable.getParticleEffect()
				entity.add(RenderableComponent(r))
				entity.add(PositionComponent())
				val pos = entity.pos()!!

				pos.position = tile
				pos.slot = slot

				Global.engine.addEntity(entity)

				entities.add(entity)
			}
		}
		else
		{
			if (sequence.targets.size == 0) return false

			val entity = Entity()

			val r = renderable.getParticleEffect()
			entity.add(RenderableComponent(r))
			entity.add(PositionComponent())
			val pos = entity.pos()!!

			pos.min = sequence.targets.minBy(Point::hashCode)!!
			pos.max = sequence.targets.maxBy(Point::hashCode)!!
			pos.slot = slot

			if (alignToVector)
			{
				pos.facing = sequence.facing
			}

			r.size[0] = (pos.max.x - pos.min.x) + 1
			r.size[1] = (pos.max.y - pos.min.y) + 1

			if (alignToVector && r is ParticleEffect && r.useFacing)
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
			val renderable = entity.renderable()!!.renderable
			if (renderable is ParticleEffect)
			{
				if (killOnEnd)
				{
					Global.engine.removeEntity(entity)
				}
				else
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

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val out = DestinationRenderableAction(sequence)
		out.renderable = renderable
		out.slot = slot
		out.killOnEnd = killOnEnd
		out.alignToVector = alignToVector
		out.entityPerTile = entityPerTile

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		entityPerTile = xmlData.getBoolean("RenderablePerTile", false)
		killOnEnd = xmlData.getBoolean("KillOnEnd", false)
		alignToVector = xmlData.getBoolean("AlignToVector", true)
	}
}

class SourceRenderableAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	var killOnEnd = true

	val entities = Array<Entity>(1)

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		val tile = sequence.source.tile()!!
		val entity = Entity()

		val r = renderable.getParticleEffect()
		entity.add(RenderableComponent(r))
		entity.add(PositionComponent())
		val pos = entity.pos()!!

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
			val renderable = entity.renderable()!!.renderable
			if (renderable is ParticleEffect)
			{
				if (killOnEnd)
				{
					Global.engine.removeEntity(entity)
				}
				else
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

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val out = SourceRenderableAction(sequence)
		out.renderable = renderable
		out.slot = slot
		out.killOnEnd = killOnEnd

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
		killOnEnd = xmlData.getBoolean("KillOnEnd", false)
	}
}

class MovementRenderableAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	lateinit var renderable: ParticleEffectDescription
	lateinit var slot: SpaceSlot
	var useLeap: Boolean = false

	lateinit var entity: Entity

	override fun enter(): Boolean
	{
		if (Global.resolveInstant) return false

		entity = Entity()

		val r = renderable.getParticleEffect()
		entity.add(RenderableComponent(r))
		entity.add(PositionComponent())
		val pos = entity.pos()!!

		val min = sequence.targets.minBy(Point::hashCode)!!
		val max = sequence.targets.maxBy(Point::hashCode)!!
		val midPoint = min + (max - min) / 2

		pos.position = sequence.level.getTileClamped(midPoint)
		pos.slot = slot

		r.rotation = getRotation(sequence.source.tile()!!, pos.position)

		val duration = end - start
		if (useLeap)
		{
			r.faceInMoveDirection = true
			r.animation = LeapAnimation.obtain().set(duration, pos.position.getPosDiff(sequence.source.tile()!!), 2f)
			r.animation = ExpandAnimation.obtain().set(duration, 0.5f, 1.5f, false)
		}
		else
		{
			r.faceInMoveDirection = true
			r.animation = MoveAnimation.obtain().set(duration, UnsmoothedPath(midPoint.getPosDiff(sequence.source.tile()!!)), Interpolation.linear)
		}

		Global.engine.addEntity(entity)

		return false
	}

	override fun exit()
	{
		if (Global.resolveInstant) return

		Global.engine.removeEntity(entity)
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val out = MovementRenderableAction(sequence)
		out.renderable = renderable
		out.useLeap = useLeap
		out.slot = slot

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		slot = SpaceSlot.valueOf(xmlData.get("Slot", "Entity")!!.toUpperCase())
		useLeap = xmlData.getBoolean("UseLeap", false)
		renderable = AssetManager.loadParticleEffect(xmlData.getChildByName("Renderable")!!)
	}
}

class ScreenShakeAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	var speed: Float = 0f
	var amount: Float = 0f

	override fun enter(): Boolean
	{
		Global.engine.render().renderer.setScreenShake(amount, speed)
		Global.engine.render().renderer.lockScreenShake()

		return false
	}

	override fun exit()
	{
		Global.engine.render().renderer.unlockScreenShake()
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val out = ScreenShakeAction(sequence)
		out.speed = speed
		out.amount = amount

		return out
	}

	override fun parse(xmlData: XmlData)
	{
		this.speed = 1f / xmlData.getFloat("Speed", 10f)
		this.amount = xmlData.getFloat("Strength", 5f)
	}
}