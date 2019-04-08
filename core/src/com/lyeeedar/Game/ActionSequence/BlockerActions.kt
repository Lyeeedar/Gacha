package com.lyeeedar.Game.ActionSequence

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.TransientComponent
import com.lyeeedar.Global
import com.lyeeedar.Util.XmlData

class BlockTurnAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	override fun enter(): Boolean
	{
		return true
	}

	override fun exit()
	{

	}

	override fun onTurn(): Boolean
	{
		return false
	}

	override fun doCopy(ability: ActionSequence): AbstractActionSequenceAction
	{
		return BlockTurnAction(ability)
	}

	override fun parse(xmlData: XmlData)
	{

	}
}

class UnlockEntityAction(ability: ActionSequence) : AbstractActionSequenceAction(ability)
{
	override fun enter(): Boolean
	{
		val entity = Entity()
		var activeAbilityComponent = Mappers.activeActionSequence.get(ability.source)
		if (activeAbilityComponent?.sequence == ability)
		{
			ability.source.remove(ActiveActionSequenceComponent::class.java)
		}
		else
		{
			activeAbilityComponent = ActiveActionSequenceComponent()
			activeAbilityComponent.sequence = ability
		}

		entity.add(TransientComponent())
		entity.add(activeAbilityComponent)
		Global.engine.addEntity(entity)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: ActionSequence): AbstractActionSequenceAction
	{
		val action = UnlockEntityAction(ability)
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}

}