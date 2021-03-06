package com.lyeeedar.Game.ActionSequence

import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Components.ActiveActionSequenceComponent
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Components.TransientComponent
import com.lyeeedar.Components.activeActionSequence
import com.lyeeedar.Global
import com.lyeeedar.Util.XmlData

class BlockTurnAction : AbstractActionSequenceAction()
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

	override fun doCopy(): AbstractActionSequenceAction
	{
		return BlockTurnAction.obtain()
	}

	override fun parse(xmlData: XmlData)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<BlockTurnAction> = object : Pool<BlockTurnAction>() {
			override fun newObject(): BlockTurnAction
			{
				return BlockTurnAction()
			}

		}

		@JvmStatic fun obtain(): BlockTurnAction
		{
			val obj = BlockTurnAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { BlockTurnAction.pool.free(this); obtained = false } }
}

class UnlockEntityAction : AbstractActionSequenceAction()
{
	override fun enter(): Boolean
	{
		val entity = EntityPool.obtain()
		var activeAbilityComponent = sequence.source.activeActionSequence()
		if (activeAbilityComponent?.sequence == sequence)
		{
			sequence.source.remove(ActiveActionSequenceComponent::class.java)
		}
		else
		{
			activeAbilityComponent = ActiveActionSequenceComponent.obtain()
			activeAbilityComponent.sequence = sequence
		}

		entity.add(TransientComponent.obtain())
		entity.add(activeAbilityComponent)
		Global.engine.addEntity(entity)

		return false
	}

	override fun exit()
	{

	}

	override fun doCopy(): AbstractActionSequenceAction
	{
		val action = UnlockEntityAction.obtain()
		return action
	}

	override fun parse(xmlData: XmlData)
	{

	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<UnlockEntityAction> = object : Pool<UnlockEntityAction>() {
			override fun newObject(): UnlockEntityAction
			{
				return UnlockEntityAction()
			}

		}

		@JvmStatic fun obtain(): UnlockEntityAction
		{
			val obj = UnlockEntityAction.pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { UnlockEntityAction.pool.free(this); obtained = false } }
}