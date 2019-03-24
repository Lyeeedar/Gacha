package com.lyeeedar.Systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.lyeeedar.AI.Tasks.TaskInterrupt
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class StatisticsSystem : AbstractSystem(Family.one(StatisticsComponent::class.java).get())
{
	val lostHpClearSpeed = 5f

	val blockEffect = AssetManager.loadParticleEffect("Block")
	val blockBrokenEffect = AssetManager.loadParticleEffect("BlockBroken")
	val hitEffect = AssetManager.loadParticleEffect("Hit")

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
			if (entity.getComponent(MarkedForDeletionComponent::class.java) == null) entity.add(MarkedForDeletionComponent())
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
			val sprite = entity.renderable()?.renderable as? Sprite

			if (sprite != null)
			{
				sprite.colourAnimation = BlinkAnimation.obtain().set(Colour(1f, 0.5f, 0.5f, 1f), sprite.colour, 0.15f, true)
			}

			val tile = entity.tile()
			if (tile != null)
			{
				val p = hitEffect.copy()
				p.size[0] = entity.pos().size
				p.size[1] = entity.pos().size

				p.addToEngine(entity.pos().position)
			}

			stats.tookDamage = false
		}

		if (stats.blockBroken)
		{
			stats.blockBroken = false

			val p = blockBrokenEffect.copy()
			p.size[0] = entity.pos().size
			p.size[1] = entity.pos().size

			p.addToEngine(entity.pos().position)

			entity.task().tasks.add(TaskInterrupt())
		}
		else if (stats.blockedDamage)
		{
			stats.blockedDamage = false

			val p = blockEffect.copy()
			p.size[0] = entity.pos().size
			p.size[1] = entity.pos().size

			p.addToEngine(entity.pos().position)
		}
	}

	override fun onTurn()
	{
		for (entity in entities)
		{
			val stat = entity.stats()!!
			stat.hp += stat.getStat(Statistic.MAXHEALTH) * stat.getStat(Statistic.REGENERATION)
		}
	}
}
