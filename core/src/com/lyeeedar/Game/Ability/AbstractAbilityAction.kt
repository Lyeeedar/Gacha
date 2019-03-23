package com.lyeeedar.Game.Ability

import com.lyeeedar.Util.XmlData

abstract class AbstractAbilityAction(val ability: Ability)
{
	var start: Float = 0f
	var end: Float = 0f

	abstract fun enter(): Boolean

	abstract fun exit()

	open fun onTurn(): Boolean
	{
		return false
	}

	fun copy(ability: Ability): AbstractAbilityAction
	{
		val newAction = doCopy(ability)
		newAction.start = start
		newAction.end = end

		return newAction
	}

	protected abstract fun doCopy(ability: Ability): AbstractAbilityAction
	abstract fun parse(xmlData: XmlData)

	companion object
	{
		fun load(xmlData: XmlData, ability: Ability): AbstractAbilityAction
		{
			val node = when (xmlData.name.toUpperCase())
			{
				"BLOCKTURN" -> BlockTurnAction(ability)
				"UNLOCKENTITY" -> UnlockEntityAction(ability)

				"DAMAGE" -> DamageAction(ability)
				"STUN" -> StunAction(ability)

				"MOVESOURCE" -> MoveSourceAction(ability)
				"PULL" -> PullAction(ability)
				"KNOCKBACK" -> KnockbackAction(ability)

				"PERMUTE" -> PermuteAction(ability)
				"SELECTALLIES" -> SelectAlliesAction(ability)
				"SELECTENEMIES" -> SelectEnemiesAction(ability)
				"SELECTRANDOM" -> SelectRandomAction(ability)
				"CLEARSELECTION" -> ClearSelectionAction(ability)

				"SOURCERENDERABLE" -> SourceRenderableAction(ability)
				"REPLACESOURCERENDERABLE" -> ReplaceSourceRenderableAction(ability)
				"SOURCEANIMATION" -> SourceAnimationAction(ability)
				"DESTINATIONRENDERABLE" -> DestinationRenderableAction(ability)
				"MOVEMENTRENDERABLE" -> MovementRenderableAction(ability)
				"SCREENSHAKE" -> ScreenShakeAction(ability)

				else -> throw Exception("Unhandled ability action type " + xmlData.getAttribute("meta:RefKey") + "!")
			}

			node.parse(xmlData)

			node.start = xmlData.getFloat("Time", 0f)
			node.end = node.start + xmlData.getFloat("Duration", 0f)

			return node
		}
	}
}