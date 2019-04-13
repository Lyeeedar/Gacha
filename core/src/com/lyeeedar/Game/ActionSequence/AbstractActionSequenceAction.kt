package com.lyeeedar.Game.ActionSequence

import com.lyeeedar.Util.XmlData

abstract class AbstractActionSequenceAction(val sequence: ActionSequence)
{
	var start: Float = 0f
	var end: Float = 0f

	abstract fun enter(): Boolean

	abstract fun exit()

	open fun onTurn(): Boolean
	{
		return false
	}

	fun copy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val newAction = doCopy(sequence)
		newAction.start = start
		newAction.end = end

		return newAction
	}

	protected abstract fun doCopy(sequence: ActionSequence): AbstractActionSequenceAction
	abstract fun parse(xmlData: XmlData)

	companion object
	{
		fun load(xmlData: XmlData, sequence: ActionSequence): AbstractActionSequenceAction
		{
			val node = when (xmlData.name.toUpperCase())
			{
				"BLOCKTURN" -> BlockTurnAction(sequence)
				"UNLOCKENTITY" -> UnlockEntityAction(sequence)

				"DAMAGE" -> DamageAction(sequence)
				"STUN" -> StunAction(sequence)
				"HEAL" -> HealAction(sequence)
				"BUFF", "DEBUFF" -> BuffAction(sequence)
				"SUMMON" -> SummonAction(sequence)

				"MOVESOURCE" -> MoveSourceAction(sequence)
				"PULL" -> PullAction(sequence)
				"KNOCKBACK" -> KnockbackAction(sequence)

				"PERMUTE" -> PermuteAction(sequence)
				"SELECTALLIES", "SELECTENEMIES", "SELECTENTITIES" -> SelectEntitiesAction(sequence)
				"SELECTTILES" -> SelectTilesAction(sequence)
				"SELECTSELF" -> SelectSelfAction(sequence)
				"LOCKTARGETS" -> LockTargetsAction(sequence)

				"SOURCERENDERABLE" -> SourceRenderableAction(sequence)
				"REPLACESOURCERENDERABLE" -> ReplaceSourceRenderableAction(sequence)
				"SOURCEANIMATION" -> SourceAnimationAction(sequence)
				"DESTINATIONRENDERABLE" -> DestinationRenderableAction(sequence)
				"MOVEMENTRENDERABLE" -> MovementRenderableAction(sequence)
				"SCREENSHAKE" -> ScreenShakeAction(sequence)

				"REPEATBEGIN" -> RepeatBegin(sequence)
				"REPEATEND" -> RepeatEnd(sequence)

				else -> throw Exception("Unhandled sequence action type " + xmlData.getAttribute("meta:RefKey") + "!")
			}

			node.parse(xmlData)

			node.start = xmlData.getFloat("Time", 0f)
			node.end = node.start + xmlData.getFloat("Duration", 0f)

			return node
		}
	}
}