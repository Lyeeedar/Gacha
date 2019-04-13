package com.lyeeedar.Game.ActionSequence

import com.lyeeedar.Util.XmlData

class RepeatBegin(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
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

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		throw UnsupportedOperationException()
	}

	override fun parse(xmlData: XmlData)
	{
		count = xmlData.getInt("Count")
	}
}

class RepeatEnd(sequence: ActionSequence) : AbstractActionSequenceAction(sequence)
{
	override fun enter(): Boolean
	{
		throw UnsupportedOperationException()
	}

	override fun exit()
	{
		throw UnsupportedOperationException()
	}

	override fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		throw UnsupportedOperationException()
	}

	override fun parse(xmlData: XmlData)
	{

	}
}