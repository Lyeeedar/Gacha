package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Util.XmlData

class ActiveAbilityComponent : AbstractComponent()
{
	lateinit var ability: Ability

	override fun parse(xml: XmlData, entity: Entity)
	{

	}
}