package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

class DialogueComponent : AbstractComponent()
{
	var text: String = ""
	var displayedText: String = ""

	var textAccumulator: Float = 0f
	var textFade: Float = 0.5f

	var turnsToShow = -1

	val alpha: Float
		get() = textFade / 0.5f

	var remove: Boolean = false
		set(value)
		{
			field = value
			textFade = 0.5f
		}

//	val createCallstack: Array<StackTraceElement>
//
//	init
//	{
//		createCallstack = Thread.currentThread().getStackTrace()
//		if (Global.release) throw Exception("Debug code needs to be removed!")
//	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DialogueComponent> = object : Pool<DialogueComponent>() {
			override fun newObject(): DialogueComponent
			{
				return DialogueComponent()
			}

		}

		@JvmStatic fun obtain(): DialogueComponent
		{
			val obj = DialogueComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { DialogueComponent.pool.free(this); obtained = false } }
	override fun reset()
	{
		text = ""
		displayedText = ""
		textAccumulator = 0f
		textFade = 0.5f
		turnsToShow = -1
		remove = false
	}
}