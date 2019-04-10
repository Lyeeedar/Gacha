package com.lyeeedar.Systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.lyeeedar.Game.Level
import kotlin.reflect.KClass

val systemList: Array<KClass<out AbstractSystem>> = arrayOf(
	ActionSequenceSystem::class,
	TaskProcessorSystem::class,
	StatisticsSystem::class,
	DeletionSystem::class,
	EventSystem::class,
	DirectionalSpriteSystem::class,
	RenderSystem::class,
	DialogueSystem::class
)

fun createEngine(): Engine
{
	val engine = Engine()

	for (system in systemList)
	{
		val instance: EntitySystem = ClassReflection.newInstance(system.java)
		engine.addSystem(instance)
	}

	return engine
}

var Engine.level: Level?
	get() = this.task().level
	set(value)
	{
		for (system in systemList)
		{
			this.getSystem(system.java)?.level = value
		}
	}

fun Engine.render() = this.getSystem(RenderSystem::class.java)
fun Engine.task() = this.getSystem(TaskProcessorSystem::class.java)
fun Engine.directionSprite() = this.getSystem(DirectionalSpriteSystem::class.java)
fun Engine.event() = this.getSystem(EventSystem::class.java)