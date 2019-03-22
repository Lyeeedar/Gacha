package com.lyeeedar.Game.Ability

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
		ability.entityLocked = false

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