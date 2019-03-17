package com.lyeeedar.AI.BehaviourTree

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Actions.*
import com.lyeeedar.AI.BehaviourTree.Conditionals.ConditionalCheckValue
import com.lyeeedar.AI.BehaviourTree.Decorators.*
import com.lyeeedar.AI.BehaviourTree.Selectors.*
import com.lyeeedar.Components.stats
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

/**
 * Created by Philip on 21-Mar-16.
 */

abstract class AbstractTreeNode()
{
	//----------------------------------------------------------------------
	var parent: AbstractNodeContainer? = null
	var state: ExecutionState = ExecutionState.NONE
	var data: ObjectMap<String, Any>? = null

	//----------------------------------------------------------------------
	fun setData(key:String, value:Any?)
	{
		val oldVal = data?.get(key)
		if (oldVal != value && oldVal is Point)
		{
			oldVal.free()
		}

		data?.put(key, value)
	}

	//----------------------------------------------------------------------
	private val variables = ObjectFloatMap<String>()
	fun getVariableMap(): ObjectFloatMap<String>
	{
		variables.clear()

		val parentmap = parent?.getVariableMap()
		if (parentmap != null)
		{
			for (pair in parentmap)
			{
				variables.put(pair.key, pair.value)
			}
		}

		for (entry in data!!)
		{
			if (entry.value is Float)
			{
				variables.put(entry.key, entry.value as Float)
			}
			else if (entry.value is Int)
			{
				variables.put(entry.key, (entry.value as Int).toFloat())
			}
			else if (entry.value is Boolean)
			{
				variables.put(entry.key, if(entry.value as Boolean) 1f else 0f)
			}
			else if (entry.value is Entity)
			{
				variables.put(entry.key, 1f)
				val stats = (entry.value as Entity).stats()
				stats.write(variables, entry.key)
			}
			else
			{
				variables.put(entry.key, 1f)
			}
		}

		return variables
	}

	//----------------------------------------------------------------------
	fun <T> getData(key:String, fallback:T? = null): T?
	{
		val parentVar = parent?.getData<T>(key, null)

		val thisVar = data?.get(key) as? T

		return thisVar ?: parentVar ?: fallback
	}

	//----------------------------------------------------------------------
	companion object
	{
		fun load(path: String, root: Boolean = false): AbstractTreeNode
		{
			val xml = getXml("AI/$path.xml")

			val rootEl = xml.getChildByName("Root")!!

			val node = AbstractTreeNode.get(rootEl.getAttribute("meta:RefKey").toUpperCase())
			if (root) node.data = ObjectMap()

			node.parse(xml)

			return node
		}

		fun get(name: String): AbstractTreeNode
		{
			val node = when(name.toUpperCase()) {

			// Selectors
				"ANY" -> SelectorAny()
				"PRIORITY" -> SelectorPriority()
				"RANDOM" -> SelectorRandom()
				"SEQUENCE" -> SelectorSequence()
				"UNTIL" -> SelectorUntil()

			// Decorators
				"DATASCOPE" -> DecoratorDataScope()
				"IMPORT" -> DecoratorImport()
				"INVERT" -> DecoratorInvert()
				"REPEAT" -> DecoratorRepeat()
				"SETSTATE" -> DecoratorSetState()

			// Actions
				"ATTACK" -> ActionAttack()
				"CLEARVALUE" -> ActionClearValue()
				"CONVERTTO" -> ActionConvertTo()
				"GETALLVISIBLE" -> ActionGetAllVisible()
				"KILL" -> ActionKill()
				"MOVETO" -> ActionMoveTo()
				"PICK" -> ActionPick()
				"SETVALUE" -> ActionSetValue()
				"WAIT" -> ActionWait()

			// Conditionals
				"CONDITIONAL" -> ConditionalCheckValue()

			// ARGH everything broke
				else -> throw RuntimeException("Invalid node type: $name")
			}

			return node
		}

	}

	//----------------------------------------------------------------------
	abstract fun evaluate(entity: Entity): ExecutionState
	abstract fun parse(xml: XmlData)
	abstract fun cancel(entity: Entity)
}
