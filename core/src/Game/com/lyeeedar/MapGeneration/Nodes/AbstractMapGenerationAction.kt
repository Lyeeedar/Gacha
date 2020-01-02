package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.copy

abstract class AbstractMapGenerationAction(val generator: MapGenerator)
{
	abstract fun execute(args: NodeArguments)
	abstract fun parse(xmlData: XmlData)
	abstract fun resolve()

	companion object
	{
		fun load(xmlData: XmlData, generator: MapGenerator): AbstractMapGenerationAction
		{
			val refKey = xmlData.name.toUpperCase()
			val action = when(refKey)
			{
				"CONDITION" -> ConditionAction(generator)
				"DATASCOPE" -> DatascopeAction(generator)
				"DEFER" -> DeferAction(generator)
				"DEFINEVARIABLE" -> DefineVariableAction(generator)
				"DIVIDE" -> DivideAction(generator)
				"FILL" -> FillAction(generator)
				"FILTER" -> FilterAction(generator)
				"FLIP" -> FlipAction(generator)
				"CHILD" -> NodeAction(generator)
				"PERPOINT" -> PerPointAction(generator)
				"REPEAT" -> RepeatAction(generator)
				"ROTATE" -> RotateAction(generator)
				"SCALE" -> ScaleAction(generator)
				"SPLIT" -> SplitAction(generator)
				"SQUIDLIBDUNGEONGENERATOR" -> SquidlibDungeonGeneratorAction(generator)
				"SQUIDLIBSECTIONGENERATOR" -> SquidlibSectionGeneratorAction(generator)
				"SQUIDLIBDENSEROOMGENERATOR" -> SquidlibDenseRoomGeneratorAction(generator)
				"SQUIDLIBFLOWINGCAVEGENERATOR" -> SquidlibFlowingCaveGeneratorAction(generator)
				"SQUIDLIBLANESMAPGENERATOR" -> SquidlibLanesMapGeneratorAction(generator)
				"SQUIDLIBORGANICMAPGENERATOR" -> SquidlibOrganicMapGeneratorAction(generator)
				"SQUIDLIBSERPENTMAPGENERATOR" -> SquidlibSerpentMapGeneratorAction(generator)
				"SQUIDLIBSYMMETRYGENERATOR" -> SquidlibSymmetryGeneratorAction(generator)
				"CHAMBERSGENERATOR" -> ChambersGeneratorAction(generator)
				"FINDROOMS" -> FindRoomsAction(generator)
				"SETNAMEDAREA" -> SetNamedAreaAction(generator)
				"SELECTNAMEDAREA" -> SelectNamedAreaAction(generator)
				"CONNECTROOMS" -> ConnectRoomsAction(generator)
				"SYMBOL" -> SymbolAction(generator)
				"TAKE" -> TakeAction(generator)
				"TRANSLATE" -> TranslateAction(generator)
				else -> throw Exception("Unknown MapGeneratorAction '$refKey'!")
			}

			action.parse(xmlData)

			return action
		}
	}
}

class NodeArguments(val area: Area, val variables: ObjectFloatMap<String>, val symbolTable: ObjectMap<Char, Symbol>)
{
	fun copy(scopeArea: Boolean = false, scopeVariables: Boolean = false, scopeSymbols: Boolean = false): NodeArguments
	{
		val area = if (scopeArea) this.area.copy() else this.area
		val variables = if (scopeVariables) this.variables.copy() else this.variables
		val symbols = if (scopeSymbols) this.symbolTable.copy() else this.symbolTable

		return NodeArguments(area, variables, symbols)
	}
}

