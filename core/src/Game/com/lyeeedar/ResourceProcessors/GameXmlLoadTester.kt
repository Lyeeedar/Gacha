package com.lyeeedar.ResourceProcessors

import com.lyeeedar.Util.XmlData

class GameXmlLoadTester
{
	companion object
	{
		fun testLoad(xml: XmlData, path: String)
		{
			when (xml.name.toUpperCase())
			{
				//"EFFECT" -> ParticleEffect.load(path.split("Particles/")[1], ParticleEffectDescription(""))
				//else -> throw RuntimeException("Unhandled path type '${xml.name}'!")
			}
		}
	}
}