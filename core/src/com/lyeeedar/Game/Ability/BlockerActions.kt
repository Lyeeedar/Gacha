package com.lyeeedar.Game.Ability

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Components.ActiveAbilityComponent
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.TransientComponent
import com.lyeeedar.Global
import com.lyeeedar.Util.XmlData

class BlockTurnAction(ability: Ability) : AbstractAbilityAction(ability)
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

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		return BlockTurnAction(ability)
	}

	override fun parse(xmlData: XmlData)
	{

	}
}

class UnlockEntityAction(ability: Ability) : AbstractAbilityAction(ability)
{
	override fun enter(): Boolean
	{
		val entity = Entity()
		var activeAbilityComponent = Mappers.activeAbility.get(ability.source)
		if (activeAbilityComponent?.ability == ability)
		{
			ability.source.remove(ActiveAbilityComponent::class.java)
		}
		else
		{
			activeAbilityComponent = ActiveAbilityComponent()
			activeAbilityComponent.ability = ability
		}

		entity.add(TransientComponent())
		entity.add(activeAbilityComponent)
		Global.engine.addEntity(entity)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(ability: Ability): AbstractAbilityAction
	{
		val action = UnlockEntityAction(ability)
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}

}