package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Game.ActionSequence.ActionSequence
import com.lyeeedar.Util.XmlData

class ActiveActionSequenceComponent() : AbstractComponent()
{
	constructor(sequence: ActionSequence) : this()
	{
		this.sequence = sequence
	}

	lateinit var sequence: ActionSequence

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}
}