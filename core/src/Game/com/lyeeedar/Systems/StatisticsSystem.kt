package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Array
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.RenderSystemWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.Statics
import ktx.actors.then

class StatisticsSystem : AbstractSystem(Family.one(StatisticsComponent::class.java).get())
{
	val lostHpClearSpeed = 5f

	val blockEffect = AssetManager.loadParticleEffect("Block")
	val blockBrokenEffect = AssetManager.loadParticleEffect("BlockBroken")
	val hitEffect = AssetManager.loadParticleEffect("Hit")

	val messageList = Array<Label>()

	override fun doUpdate(deltaTime: Float)
	{
		for (entity in entities)
		{
			processEntity(entity, deltaTime)
		}
	}

	fun processEntity(entity: Entity, deltaTime: Float)
	{
		val stats = entity.stats()!!

		if (stats.hp <= 0)
		{
			if (entity.getComponent(MarkedForDeletionComponent::class.java) == null) entity.add(MarkedForDeletionComponent.obtain())
		}

		if (stats.lostHp > 0)
		{
			stats.lostHp -= deltaTime * lostHpClearSpeed * (stats.maxLostHp / 2f)
			if (stats.lostHp < 0f)
			{
				stats.lostHp = 0f
				stats.maxLostHp = 0f
			}
		}

		if (stats.tookDamage)
		{
			if (!Global.resolveInstant)
			{
				val sprite = entity.renderable()?.renderable as? Sprite

				if (sprite != null)
				{
					sprite.colourAnimation = BlinkAnimation.obtain().set(Colour(1f, 0.5f, 0.5f, 1f), sprite.colour, 0.15f, true)
				}

				val tile = entity.tile()
				if (tile != null)
				{
					val p = hitEffect.getParticleEffect()
					p.size[0] = entity.pos().size
					p.size[1] = entity.pos().size

					p.addToEngine(entity.pos().position)
				}
			}

			stats.tookDamage = false
		}

		if (stats.blockBroken)
		{
			stats.blockBroken = false

			if (!Global.resolveInstant)
			{
				val p = blockBrokenEffect.getParticleEffect()
				p.size[0] = entity.pos().size
				p.size[1] = entity.pos().size

				p.addToEngine(entity.pos().position)
			}

			entity.task().tasks.add(TaskInterrupt())
		}
		else if (stats.blockedDamage)
		{
			stats.blockedDamage = false

			if (!Global.resolveInstant)
			{
				val p = blockEffect.getParticleEffect()
				p.size[0] = entity.pos().size
				p.size[1] = entity.pos().size

				p.addToEngine(entity.pos().position)
			}
		}

		if (!Global.resolveInstant)
		{
			for (message in stats.messagesToShow)
			{
				val render = Global.engine.render()!!
				val pos = RenderSystemWidget.instance.pointToScreenspace(entity.pos()!!.position)
				pos.add(render.tileSize / 2f + Random.random(-2, 2).toFloat(), render.tileSize + Random.random(-2, 2).toFloat())

				val label = Label(message.text, Statics.skin, "popup")
				label.color = message.colour.color()
				label.setFontScale(message.size)
				label.rotation = -60f
				label.setPosition(pos.x - label.prefWidth / 2f, pos.y)

				val sequence =
					alpha(0f) then
						fadeIn(0.1f) then
						parallel(
							moveBy(2f, 3f, 1f),
							sequence(delay(0.5f), fadeOut(0.5f))) then
						lambda { messageList.removeValue(label, true) } then
						removeActor()

				label.addAction(sequence)

				val width = label.prefWidth
				if (pos.x + width > Statics.stage.width)
				{
					label.setPosition(Statics.stage.width - width - 20, pos.y)
				}

				messageList.add(label)
				Statics.stage.addActor(label)
			}
		}

		for (message in stats.messagesToShow)
		{
			message.free()
		}
		stats.messagesToShow.clear()
	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val stats = entity.stats()!!
			val regenAmount = stats.getStat(Statistic.MAXHP) * stats.getStat(Statistic.REGENERATION)
			if (regenAmount > 0f)
			{
				stats.regenerate(regenAmount)
			}
			else if (regenAmount < 0f)
			{
				stats.dealDamage(regenAmount, false)
			}

			val itr = stats.buffs.iterator()
			while (itr.hasNext())
			{
				val buff = itr.next()

				if (buff.duration > 0)
				{
					buff.duration--
					if (buff.duration == 0)
					{
						itr.remove()
					}
				}
			}
		}
	}
}
