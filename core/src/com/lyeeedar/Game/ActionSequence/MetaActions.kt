package com.lyeeedar.Game.ActionSequence

import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.XmlData

class RepeatBegin() : AbstractActionSequenceAction()
{
	var count: Int = 1

	override fun enter(): Boolean
	{
		throw UnsupportedOperationException()
	}

	override fun exit()
	{
		throw UnsupportedOperationException()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		throw UnsupportedOperationException()
	}

	override fun parse(xmlData: XmlData)
	{
		count = xmlData.getInt("Count")
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<RepeatBegin> = object : Pool<RepeatBegin>() {
			override fun newObject(): RepeatBegin
			{
				return RepeatBegin()
			}

		}

		@JvmStatic fun obtain(): RepeatBegin
		{
			val obj = RepeatBegin.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { RepeatBegin.pool.free(this); obtained = false } }
}

class RepeatEnd() : AbstractActionSequenceAction()
{
	override fun enter(): Boolean
	{
		throw UnsupportedOperationException()
	}

	override fun exit()
	{
		throw UnsupportedOperationException()
	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		throw UnsupportedOperationException()
	}

	override fun parse(xmlData: XmlData)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<RepeatEnd> = object : Pool<RepeatEnd>() {
			override fun newObject(): RepeatEnd
			{
				return RepeatEnd()
			}

		}

		@JvmStatic fun obtain(): RepeatEnd
		{
			val obj = RepeatEnd.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { RepeatEnd.pool.free(this); obtained = false } }
}