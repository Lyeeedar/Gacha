package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidgrid.mapping.RoomFinder

class FindRoomsAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	lateinit var roomName: String
	lateinit var corridorName: String

	override fun execute(args: NodeArguments)
	{
		val asChars = Array<CharArray>(args.area.width) { x -> CharArray(args.area.height) { y -> args.area[x, y]!!.char } }
		val roomFinder = RoomFinder(asChars)

		if (roomName.isNotBlank())
		{
			val rooms = roomFinder.findRooms()
			var roomsStore = generator.namedAreas[roomName]
			if (roomsStore == null)
			{
				roomsStore = com.badlogic.gdx.utils.Array()
				generator.namedAreas[roomName] = roomsStore
			}
			for (room in rooms)
			{
				val newArea = args.area.newAreaFromCharGrid(room)
				roomsStore.add(newArea)
			}
		}

		if (corridorName.isNotBlank())
		{
			val corridors = roomFinder.findCorridors()
			var corridorsStore = generator.namedAreas[corridorName]
			if (corridorsStore == null)
			{
				corridorsStore = com.badlogic.gdx.utils.Array()
				generator.namedAreas[corridorName] = corridorsStore
			}
			for (corridor in corridors)
			{
				val newArea = args.area.newAreaFromCharGrid(corridor)
				corridorsStore.add(newArea)
			}
		}
	}

	override fun parse(xmlData: XmlData)
	{
		roomName = xmlData.get("RoomName", "")!!
		corridorName = xmlData.get("CorridorName", "")!!
	}

	override fun resolve()
	{

	}
}