package com.lyeeedar.Game.ActionSequence

import com.lyeeedar.Util.XmlData

abstract class AbstractActionSequenceAction
{
	lateinit var sequence: ActionSequence

	var start: Float = 0f
	var end: Float = 0f

	fun set(sequence: ActionSequence, start: Float, end: Float): AbstractActionSequenceAction
	{
		this.sequence = sequence
		this.start = start
		this.end = end

		return this
	}

	abstract fun enter(): Boolean

	abstract fun exit()

	open fun onTurn(): Boolean
	{
		return false
	}

	fun copy(sequence: ActionSequence): AbstractActionSequenceAction
	{
		val newAction = doCopy()
		newAction.set(sequence, start, end)

		return newAction
	}

	protected abstract fun doCopy(): AbstractActionSequenceAction
	abstract fun parse(xmlData: XmlData)

	abstract fun free()

	companion object
	{
		fun load(xmlData: XmlData, sequence: ActionSequence): AbstractActionSequenceAction
		{
			val node = when (xmlData.name.toUpperCase())
			{
				"BLOCKTURN" -> BlockTurnAction.obtain()
				"UNLOCKENTITY" -> UnlockEntityAction.obtain()

				"DAMAGE" -> DamageAction.obtain()
				"STUN" -> StunAction.obtain()
				"HEAL" -> HealAction.obtain()
				"BUFF", "DEBUFF" -> BuffAction.obtain()
				"SUMMON" -> SummonAction.obtain()
				"REPLACEATTACK" -> ReplaceAttackAction.obtain()
				"MODIFYBUFF", "MODIFYDEBUFF" -> ModifyBuff.obtain()

				"MOVESOURCE" -> MoveSourceAction.obtain()
				"PULL" -> PullAction.obtain()
				"KNOCKBACK" -> KnockbackAction.obtain()

				"PERMUTE" -> PermuteAction.obtain()
				"SELECTALLIES", "SELECTENEMIES", "SELECTENTITIES" -> SelectEntitiesAction.obtain()
				"SELECTTILES" -> SelectTilesAction.obtain()
				"SELECTSELF" -> SelectSelfAction.obtain()
				"LOCKTARGETS" -> LockTargetsAction.obtain()
				"STORETARGETS" -> StoreTargets.obtain()
				"RESTORETARGETS" -> RestoreTargets.obtain()

				"SOURCERENDERABLE" -> SourceRenderableAction.obtain()
				"REPLACESOURCERENDERABLE" -> ReplaceSourceRenderableAction.obtain()
				"ATTACHTOENTITYRENDERABLE" -> AttachToEntityRenderableAction.obtain()
				"SOURCEANIMATION" -> SourceAnimationAction.obtain()
				"DESTINATIONRENDERABLE" -> DestinationRenderableAction.obtain()
				"MOVEMENTRENDERABLE" -> MovementRenderableAction.obtain()
				"SCREENSHAKE" -> ScreenShakeAction.obtain()

				"REPEATBEGIN" -> RepeatBegin.obtain()
				"REPEATEND" -> RepeatEnd.obtain()

				else -> throw Exception("Unhandled sequence action type " + xmlData.getAttribute("meta:RefKey") + "!")
			}

			node.set(sequence, 0f, 0f)
			node.parse(xmlData)

			node.start = xmlData.getFloat("Time", 0f)
			node.end = node.start + xmlData.getFloat("Duration", 0f)

			return node
		}
	}
}