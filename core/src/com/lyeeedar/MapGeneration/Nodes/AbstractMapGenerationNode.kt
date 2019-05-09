package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.copy

abstract class AbstractMapGenerationNode(val generator: MapGenerator)
{
	abstract fun execute(args: NodeArguments)
	abstract fun parse(xmlData: XmlData)
	abstract fun resolve()
}

class NodeArguments(val area: Area, val defines: ObjectMap<String, String>, val variables: ObjectFloatMap<String>, val symbolTable: ObjectMap<Char, Symbol>)
{
	fun copy(scopeArea: Boolean = false, scopeDefines: Boolean = false, scopeVariables: Boolean = false, scopeSymbols: Boolean = false): NodeArguments
	{
		val area = if (scopeArea) this.area.copy() else this.area
		val defines = if (scopeDefines) this.defines.copy() else this.defines
		val variables = if (scopeVariables) this.variables.copy() else this.variables
		val symbols = if (scopeSymbols) this.symbolTable.copy() else this.symbolTable

		return NodeArguments(area, defines, variables, symbols)
	}
}

